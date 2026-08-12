# Spec: 背压与多层限流（07 — 三维挂点 + 过载两档）

Status: ready-for-agent
源决策: wayfinder「production-readiness」07 号票（grilling 收口，决策=做三维背压与限流、网关入口 QPS 不做）
里程碑: M1 稳定基线（路线图「03 M1 → 05 → 06/07/08」；03/05/06 已收口，本机制消费其 liveSessions 台账、TurnGate 在途信号、ResilienceAdvisor 切面与 usage 采集先例）

---

## Problem Statement

单实例不周山在**量级失控**面前目前没有任何闸门：

- **spawn 无上限**：`AgentRuntime.spawn()` 只有「同 sessionId 已活跃」（`SessionAlreadyActiveException`）与「实例 drain 中」（`RuntimeDrainingException`）两类拒绝，**没有容量语义的拒绝**——调用方可以无限 spawn，每个会话一份虚拟线程执行器 + 租约心跳 + 缓冲写，实例资源（内存、DB 连接池、模型客户端连接）被静默耗尽，直到 OOM 或下游超时雪崩才暴露。
- **工具扇出参数硬编码**：执行脊柱 `HarnessToolCallingManager` 的每轮并发上限（8）与工具超时（60s）写死在装配处，没有任何 yml/properties 接线——单会话一轮 fan-out 打爆下游服务时，用户连调参的入口都没有。
- **模型出向无速率保护**：`ResilienceAdvisor` 只有「出事之后」的重试/超时/错误分类，没有「出事之前」的速率约束——provider 的 RPM/TPM 配额完全靠自觉，多会话并发时 429 风暴先打满重试预算再拖垮轮次时限；重试本身还放大出向流量。
- **过载语义缺位**：三维都没有「排队等一等 vs 快速失败」的策略概念，更没有「等多久、怎么拒、拒了之后调用方拿到什么错误」的明确契约。
- **限流处置不可观测**：SRE 无法从事件流回答「这个实例现在离容量多远、刚才拒了几个 spawn、模型桶是不是顶满了」。

从用户视角：平台集成者想让实例在过载时**体面地拒**（明确错误、可重试、有事件留痕），而不是**默默地垮**；想让 provider 配额成为配置项而不是祈祷；SRE 想在 observability 里看清每次限流处置。

## Solution

不周山落地**三维背压与限流**，聚焦「出向保护 + 资源保护」，**不新增模块**——spawn 闸与扇出闸归 core 扩展，模型双桶归 `buzhou-resilience` 扩展（限流是韧性的前哨，与 03 同模块）：

- **维度① spawn 并发会话上限**：复用 06 交付的 `liveSessions` 活跃会话台账计数；超限时按过载策略处置——**有界排队**（默认，等待其他会话 close 空位，带超时）或**快速失败**；最终拒绝抛新增容量异常（携带当前/上限计数上下文），调用方据此路由或重试。
- **维度② 每会话工具扇出上限**：把脊柱既有的每轮并发信号量与工具超时**接线上配置**（消除硬编码），排队获取许可带超时、超时拒绝该工具调用（返回错误结果而非静默吊死）。
- **维度③ 每模型 RPM+TPM 双桶**：`buzhou-resilience` 新增令牌桶限流器，挂在 ResilienceAdvisor 同位的模型调用前切面；**RPM 调用前扣减**，**TPM 调用后按实际 usage 记账 + 下次调用前预检**（诚实语义=平均速率保护，不防单尖峰，文档写清）；按 modelName 分桶，单进程内存实现。
- **过载语义两档统一**：三维共用「有界排队（默认，带超时）/ 快速失败」策略枚举；拒绝 = 调用方可重试的明确错误 + 事件进 observability 既有通道；**框架自限流拒绝不经重试管线**（与 provider 429 的可重试语义严格区分，避免重试放大拥塞）。
- **默认姿态**：机制默认装配，**各维度阈值默认不限（null），显式配置才生效**——不设可能误伤生产的魔法默认值。
- **可接管性不受影响**：排队/拒绝只发生在 spawn 与调用边界，不做 interrupt/rollback（与 06 drain、租约单活跃语义正交不冲突）。

完整闭环：**调用方压测流量 → spawn 闸按台账计数裁决（排队/放行/拒）→ 轮次内脊柱扇出按许可放行 → 模型调用前 RPM 桶预检、调用后 TPM 按 usage 记账 → 超限拒绝抛明确异常 + 事件留痕 → SRE 在事件流看到全部处置**。

## User Stories

1. 作为平台集成者，我希望配置**实例级并发活跃会话上限**，这样单实例不会因无限 spawn 耗尽内存与下游连接。
2. 作为平台集成者，我希望 spawn 超限时默认**有界排队**（等已有会话 close 空位、带超时），这样突发流量被削峰而非直接拒绝。
3. 作为平台集成者，我希望排队可配**超时**，超时后抛**携带当前/上限计数的明确异常**，这样我的路由层可以把调用导向其他实例或稍后重试。
4. 作为平台集成者，我希望可切**快速失败**档（不排队直接拒），这样强实时场景不被排队延迟拖累。
5. 作为平台集成者，我希望容量拒绝异常与「同 sessionId 已活跃」「drain 中」**语义区分**（独立异常类型），这样调用方可以只对容量拒绝做重试。
6. 作为平台集成者，我希望 spawn 排队**不占用租约**（拿到空位后才走正常 spawn 全流程），这样排队期间不产生半注册状态的会话。
7. 作为平台集成者，我希望**drain 中的实例排队中的 spawn 一并被拒**，这样 06 drain 协议不被排队请求卡住。
8. 作为 Agent 应用开发者，我希望脊柱**每轮工具并发上限可配置**（消除硬编码 8），这样我可以按下游承受能力调参。
9. 作为 Agent 应用开发者，我希望工具获取扇出许可**带超时**，超时后该工具调用返回**错误结果**（进工具结果通道，模型可见失败），而不是静默吊死整个轮次。
10. 作为 Agent 应用开发者，我希望**工具超时时间可配置**（消除硬编码 60s），这样慢工具场景可调。
11. 作为平台集成者，我希望配置**每模型 RPM 上限**（令牌桶，按 modelName 分桶），这样多会话并发不触发 provider 429 风暴。
12. 作为平台集成者，我希望配置**每模型 TPM 上限**，按实际 usage 记账，这样 token 配额同样受保护。
13. 作为平台集成者，我希望模型调用**先过限流再进重试管线**，这样速率约束不被重试放大绕过。
14. 作为 Agent 应用开发者，我希望**框架自限流的拒绝不被重试**（与 provider 429 尊重 Retry-After 的重试严格区分），这样重试不放大自拥塞。
15. 作为平台集成者，我希望模型限流同样支持**排队/快速失败两档**（默认排队带超时），语义与 spawn 闸一致。
16. 作为 SRE，我希望全部限流处置**事件留痕**（spawn 排队/拒绝、扇出许可超时、模型桶等待/拒绝，带维度与计数），这样过载处置可审计。
17. 作为 SRE，我希望限流配置**单进程内存语义如实文档化**（每实例配额=总配额/实例数由配置表达），这样我多实例部署时知道怎么折算。
18. 作为 Agent 应用开发者，我希望三维阈值**默认不限、显式配置才生效**，这样引入升级不改变既有行为。
19. 作为平台集成者，我希望一键关闭/调参走 **yml 配置**（core 侧与 resilience 侧各自前缀），特殊部署可回退。
20. 作为 Harness 集成者，我希望本机制**不新增模块**（core 扩展 spawn/脊柱 + resilience 扩展双桶），17 模块星形依赖不变。
21. 作为平台架构者，我知晓**网关入口 QPS 不做**（K8s/网关标准职责）、**分布式精确限流不做**（不引 Redis 强依赖），框架只管单实例出向与资源保护。
22. 作为平台架构者，我知晓**单用户/单租户配额归 11 成本治理与 21 多租户**——本机制管瞬时速率，不管累计闸门。
23. 作为 Agent 应用开发者，我希望流式调用的 TPM 记账**按流末尾聚合 usage**（ObservabilityAdvisor 先例），流式与非流式语义一致。
24. 作为 Agent 应用开发者，我希望公共 API 变更（容量异常类型、配置项）**全部 additive**，既有集成源码/二进制兼容。
25. 作为平台集成者，我希望「与动态预算的区分」**文档写清**——动态预算管单会话上下文窗口怎么分，本机制管跨会话速率与并发，正交不重叠。

## Implementation Decisions

### 改动面与定位

- **core 扩展 + resilience 扩展，不新增模块**（07 票与路线图口径一致：「core（spawn/脊柱）+ resilience 模块（双桶 Advisor）」）。三处挂点：spawn 闸（`DefaultAgentRuntime`）、扇出闸（`HarnessToolCallingManager` 配置接线）、模型双桶（`buzhou-resilience` 新增限流器，与 `ResilienceAdvisor` 同切面位）。
- **不新增持久化 SPI、不新增事件通道**：限流状态全部单进程内存；事件走既有 `SessionEvent` 字符串 type + `emitEvent` 通道。
- **公共 API 变更（全部 additive，PR 须说明兼容性）**：新增容量拒绝异常类型（参照 `RuntimeDrainingException` 形态，携带 sessionId/当前计数/上限上下文）；`SpawnOptions` 不动（排队语义由 runtime 配置驱动，非按 spawn 调用传入）。

### 维度① spawn 并发会话上限（core）

- **计数源（决策）**：复用 06 交付的 `liveSessions` 台账（`ConcurrentHashMap`，spawn 注册 / close 注销），不引入第二份会话计数。
- **裁决点（决策）**：`DefaultAgentRuntime.spawn` 入口处、租约获取**之前**裁决——排队不持有租约，拿到空位后才走既有 doSpawn 全流程（租约 → 装配 → 注册）。
- **排队实现（决策）**：虚拟线程 + 信号量/条件等待（对齐脊柱 `Semaphore` 手法），容量空位由会话 close 释放时通知；**禁止**轮询 sleep；等待有界（acquire-timeout），超时即拒绝。
- **拒绝异常（决策）**：新增 `SessionCapacityExceededException`（或同义命名），message 带 sessionId + 当前活跃数 + 上限 + 排队耗时（异常规约）；与 `SessionAlreadyActiveException`/`RuntimeDrainingException` 平级独立，调用方可按类型分流。
- **与 drain 的交互（决策）**：drain 拒新判定**先于**容量裁决（drain 中一律拒，含排队中的等待者被唤醒拒绝）——排队等待必须响应 drain 状态变化，不可睡死在容量信号量上。
- **steal 语义**：`spawn(steal=true)` 是接管路径（已活跃会话易主），**不占新容量**——实现期确认 steal 绕过容量计数或先释放原持有再计，勿把接管误判为超限。

### 维度② 每会话工具扇出上限（core）

- **配置接线（决策）**：`HarnessAssembler` 中硬编码的每轮并发上限（8）与工具超时（60s）改为从 properties 注入，**默认保持现值**（行为不变，只消除硬编码；现值抽为命名常量）。
- **许可超时（决策，新增行为）**：扇出许可获取从无限 `acquire()` 改为有界 `tryAcquire(timeout)`，超时后该工具调用返回**错误结果**（走既有工具错误结果通道，模型可见「工具过载未执行」语义），不阻断其他工具、不吊死轮次；许可超时独立配置项。
- **串行组语义不变**：既有 serialGroups 互斥逻辑不动。

### 维度③ 每模型 RPM+TPM 双桶（buzhou-resilience）

- **挂点（决策）**：新增限流器在模型调用前切面执行（ResilienceAdvisor 同位，**先于重试/超时包裹**——限流裁决在最外层，重试的每次尝试同样过桶）；order 相对 03 既有逻辑的关系实现期按「限流是韧性前哨」定值并注释。
- **桶模型（决策）**：令牌桶，按 modelName 分桶（modelName 取自运行时配置，同 ResilienceAdvisor 可见的模型标识）；单进程 `ConcurrentHashMap` 持有，不引外部依赖。
- **RPM（决策）**：调用前预检 + 扣减；桶空按过载策略排队（有界带超时）或拒绝。
- **TPM（决策，已确认）**：**调用后按实际 usage 记账 + 下次调用前预检**——`chatResponse.getMetadata().getUsage()`（ObservabilityAdvisor 现成先例）；流式在流末尾聚合 usage 记账（accumulateStreamChunk 先例）。诚实语义写进文档：**TPM 是平均速率保护，不防单次尖峰越限**。
- **自限流拒绝与重试的边界（决策）**：框架自限流拒绝抛独立异常（如 `ModelRateLimitExceededException`），**不进入重试分类**（错误分类仅作用于 provider 侧失败；自限流拒绝直接上抛）；provider 429 维持既有 RATE_LIMIT 分类 + Retry-After 重试不变。实现期确认两类异常在 `onModelError` Hook 切面均可被用户兜底（Replace/Block）。
- **usage 缺失**：provider 不返回 usage 时 TPM 桶只记 RPM 语义（记账 0），如实记事件/日志，不伪造估值。

### 过载语义两档（三维统一）

- 策略枚举：`queue`（有界排队 + 超时，默认）/ `fail-fast`；每维度独立可配。
- 拒绝统一契约：明确异常类型 + 事件进 observability；异常 message 带维度、当前值、上限、已等待时长。

### 事件进 observability

- 新增普通观测事件（字符串 type，参照 `drain.*`/`retry-*` 先例）：`backpressure.spawn-queued`（当前活跃/上限）、`backpressure.spawn-rejected`（原因：超时/fail-fast/drain 唤醒）、`backpressure.tool-permit-timeout`（工具名）、`backpressure.model-throttled`（modelName + 桶维度 RPM/TPM + 等待时长）、`backpressure.model-rejected`。
- 经会话/runtime 既有事件通道发出；spawn 闸事件发生在会话建立前，经 runtime 级发布路径（参照 drain-started/finished 的 hookEnv 直发先例）。

### 配置范围

- **core 侧** `@ConfigurationProperties(prefix="buzhou.backpressure")`（record + compact constructor + boxed null=不限，对齐 `BuzhouShutdownProperties` 模板）：`max-concurrent-sessions`（默认 null=不限）/ `spawn-queue-timeout` / `spawn-overload-policy` / `tool.max-concurrent-per-turn`（默认现值 8 常量化）/ `tool.tool-timeout`（默认现值 60s 常量化）/ `tool.permit-acquire-timeout` / `tool.overload-policy`。
- **resilience 侧**：`ResilienceProperties` 内嵌 `RateLimit` 组（前缀 `buzhou.resilience.rate-limit`）：`requests-per-minute` / `tokens-per-minute`（默认 null=不限）/ `queue-timeout` / `overload-policy`。
- **绑定级/工具级覆盖不在本期**（同 03/05/06 M1 口径：policy 消费管线打通前只到默认+yml 两层；07 票「策略走 policy 四层」的绑定级诉求留待管线打通后接入，如实标注）。

### 工程纪律

- 不新增模块、不新增 SPI、不引外部依赖（桶与闸全内存实现）；行为变更带测试（CONTRIBUTING 约定）。
- 阈值默认 null=不限；保留的现值（8/60s）抽命名常量，禁魔法数字。
- 借鉴（一手链接见 `docs/production-readiness/references.md` 07 条目）：LangChain InMemoryRateLimiter（令牌桶挂模型层；超越其仅 RPM 边界到 RPM+TPM 双桶）；LangGraph Platform multitask_strategy（只借 reject/enqueue 两档裁决枚举）；CrewAI max_rpm（数值化速率护栏）。

## Testing Decisions

### 什么是好测试

只测**外部行为**——spawn 的排队/拒绝语义与异常类型、工具调用计数与错误结果、模型调用计数、事件流——不测桶内部状态、信号量私有字段。「是否排队、是否拒绝、TPM 是否按 usage 记账」一律从外部观察判定（同 `CrashRecoveryEndToEndTest`/`ResilienceEndToEndTest` 的 e2e 哲学）。全部用 `CountDownLatch`/有界轮询保证确定性，**不用** wall-clock sleep。

### 缝合点（已与 owner 确认：2 个，全部复用既有缝合点形态，不新增测试基础设施）

1. **主缝合点（最高、复用既有 e2e 形态）——端到端三维度测试**：
   - 复用 `CrashRecoveryEndToEndTest`/`GracefulShutdownEndToEndTest` 装配与 `ResilienceEndToEndTest` 模型侧手法：`Buzhou.runtime(model, stores, RuntimeConfig)` + core test-jar `ScriptedChatModel` + `CountDownLatch` 阻塞工具（带计数器）。
   - 经真实 runtime 驱动，断言：
     ① **spawn 闸**——上限=1 时第二 spawn 排队（第一会话 close 后放行成功）；fail-fast 档直接抛容量异常；排队超时抛异常且 message 带计数上下文；drain 中排队者被唤醒拒绝；
     ② **扇出闸**——小每轮并发上限下并行工具调用被许可串行化（计数器佐证）；许可超时后该工具返回错误结果而轮次正常完结；
     ③ **模型双桶**——低 RPM 下第 N 次调用排队/拒绝（ScriptedChatModel 调用计数 + `seenPrompts`）；预排 usage 后 TPM 记账生效（下次调用被 TPM 桶拦）；自限流拒绝**不触发重试**（调用计数不放大）；流式调用 TPM 末尾记账；
     ④ **事件流**——`backpressure.*` 事件按序、计数正确；
     ⑤ **默认姿态**——不配置任何阈值时行为与现状完全一致（回归断言）。
   - 先验：`CrashRecoveryEndToEndTest`（双会话/latch 阻塞工具/租约交接）、`ResilienceEndToEndTest`（ScriptedChatModel 预排异常与调用计数）、`GracefulShutdownEndToEndTest`（drain 交互断言形态）、`HarnessToolCallingManagerTest`（扇出计数手法）。

2. **装配缝合点（复用既有 autoconfig 测试形态）——配置绑定与条件装配测试**：
   - 复用 `BuzhouCoreAutoConfigurationTest`/`BuzhouResilienceAutoConfigurationTest` 的 `ApplicationContextRunner` 形态，断言：
     ① `buzhou.backpressure.*` 各阈值绑定生效、缺省 null=不限；
     ② `buzhou.resilience.rate-limit.*` 绑定生效；
     ③ 脊柱硬编码参数改由配置驱动（配置值落到装配后的 manager 行为上，经 runtime 行为断言而非反射读字段）。
   - 先验：上述两个 autoconfig 测试类。

### 被测模块

- `buzhou-core`（spawn 闸 + 扇出闸配置接线 + core 侧 properties）。
- `buzhou-resilience`（双桶限流器 + ResilienceProperties 扩展）。
- 不涉及新模块；store 无语义变更；observability 模块不改（事件走既有通道）。

## Out of Scope

- **网关入口 QPS 限流**——K8s/网关标准职责，07 票明确不做。
- **单用户/单租户配额**——累计闸门归 11 成本治理与 21 多租户；本机制只管瞬时速率与并发。
- **分布式精确限流**——不引 Redis 强依赖；多实例部署按「每实例配额=总配额/实例数」配置折算，如实文档化。
- **429 自适应降速（AIMD）**——07 票留 Spec 期评估项，本期不做；provider 429 维持既有重试语义。
- **过载 interrupt/rollback**——与 06 drain、租约单活跃语义冲突，07 票明确不做。
- **绑定级/工具级 policy 消费**——同 03/05/06 M1 口径，待 policy 消费管线打通。
- **TPM 调用前预估预占**——已确认走事后记账+下次预检；预估预占（按 maxTokens）为潜在增强，不在本期。
- **08 死循环检测**——M1 同族独立机制，各自立项（共享「双窗口计数 + 软信号注入 + 硬顶」形态时的收敛留 M2）。
- **跨实例容量协调**（集群级会话上限）——编排层职责；本机制只交付单实例原语。

## Further Notes

- **管辖 ADR**：wayfinder「production-readiness」07 号票（决策=做三维、过载两档、网关 QPS 不做）。路线图落点：`docs/production-readiness/README.md` M1 行「07 背压与多层限流 | 做 | core（spawn/脊柱）+ resilience 模块（双桶 Advisor）」。
- **与既有机制的接口清单（均已交付，直接消费）**：`liveSessions` 台账与 `TurnGate` 在途信号（06，spawn 闸计数源）；`RuntimeDrainingException`（拒绝异常形态先例）；`HarnessToolCallingManager` 的 `turnPermits`/`toolTimeout`（扇出闸接线对象）；`ResilienceAdvisor` 切面与 `ModelCallInFlight`（双桶挂点）；`ObservabilityAdvisor` usage 读取与流式聚合（TPM 记账先例）；`SessionAssemblyContext.emitEvent`（事件通道）。
- **实现期须核验的开放项**：
  1. **drain 与排队 spawn 的唤醒顺序**：排队等待者须在 drain 置位时被唤醒拒绝——实现期确认 06 drain 状态对容量等待可见（避免排队线程睡死）。
  2. **steal 与容量计数**：`spawn(steal=true)` 接管路径是否占容量——确认不误判超限。
  3. **双桶与重试的切面次序**：限流裁决须先于重试包裹、且重试每次尝试重新过桶——实现期按 ResilienceAdvisor 内部结构定值并注释。
  4. **modelName 分桶键来源**：确认 advisor 侧可见的模型标识与配置键一致（多模型部署各自成桶）。
  5. **usage 缺失的 TPM 行为**：provider 不返回 usage 时的记账语义（记 0 + 留痕），实现期按 ScriptedChatModel 可模拟的形态定测试。
- **未来集成点（设计时预留，不在本期实现）**：11 成本治理的预算闸门与双桶共享「速率/累计」计数形态；21 多租户按租户分桶（桶键加 tenantId 维度）；07 票留项 AIMD 自适应降速；与 08 死循环检测在 M2 收敛「失控防护家族」统一形态（双窗口计数 + 软信号 Attachment 注入 + 硬顶阻断）。
- **文档交付**：「与动态预算的区分」（单会话窗口分配 vs 跨会话速率并发，正交）与「每实例配额折算」（总配额/实例数）写入机制文档；三维配置项全表 + 事件类型全表落 docs/spec 同步（「改机制先改 Spec」）。
