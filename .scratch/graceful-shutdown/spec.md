# Spec: 优雅停机与会话 drain（06 — drain 协议 + SmartLifecycle 四步清单）

Status: ready-for-agent
源决策: wayfinder「production-readiness」06 号票（grilling 收口，决策=做 drain 协议、不做显式跨实例迁移）
里程碑: M1 稳定基线（路线图「03 M1 → 05 → 06/07/08」；05 已收口，本机制消费其 EXIT 档 flush 钩子与租约心跳）

---

## Problem Statement

滚动发布 / 重启 / 缩容时，**在途会话**目前没有任何受控处置路径：

- **进程退出即硬断**：JVM 收到停机信号后直接退出——正在执行的轮次（在途模型调用、在途工具调用）被硬生生掐断，副作用工具可能停在「已执行、结果未落库」窗口；虽然 05 的崩溃恢复 + 幂等兜底了正确性，但**受控停机本可以不走崩溃路径**——现在却和真崩溃毫无区别。
- **runtime 无会话台账**：`AgentRuntime` 只有 `spawn()`，不追踪已 spawn 会话、无 runtime 级关闭入口——框架连「现在有哪些活跃会话」都回答不了，drain 无从下手。
- **无 drain 协议**：停机信号后没有「拒新会话 → 等在途轮次完结 → 超时强杀 → flush 缓冲写 → 排空观测管线」的有序步骤；`EXIT` 持久化档的缓冲写（05 已交付 flush 钩子）在停机时无人触发，选 `EXIT` 档的部署在每次滚动发布都丢整轮。
- **与 Spring Boot graceful shutdown 无衔接**：Boot 4 的 graceful shutdown 只管 web 请求生命周期（02 号票底座事实），不周山的会话生命周期（可能横跨多分钟的长轮次、虚拟线程 daemon 语义下 JVM 退出不等后台任务）完全裸露。
- **停机过程不可观测**：SRE 无法从事件流回答「这次发布 drain 了多久、几个会话被等完、几个被强杀、flush 是否完成」。

从用户视角：平台集成者只想让滚动发布成为**无聊的日常操作**——实例退出前把在途轮次体面地收完（或按预算强杀）、把缓冲写落盘、把租约释放，让调用方在实例 B 用同 `sessionId` 无缝续接；SRE 想在 observability 里看清每次停机处置的全过程。

## Solution

不周山在 core 内补全**会话级 drain 协议**，**不新增模块、不新增持久化 SPI**，复用既有「五 SPI + 租约 + 消息即检查点」底座与 05 交付的 EXIT 档 flush 钩子：

- **会话台账（基础项）**：runtime 追踪活跃会话（spawn 注册、close 注销），这是「拒新 + 等在途」的前提。
- **drain 三步协议**（粒度 = **当前轮次完结**，与微压缩「完结轮次」原子单位对齐；会话可能无限长，不等整个会话结束）：
  1. **拒新**：drain 开始后 `spawn()` 明确拒绝（新异常类型），调用方可路由到其他实例；
  2. **等在途**：等待每个活跃会话的**当前轮次**完结（允许会话在轮次边界被关闭，不掐断轮内执行）；
  3. **超时强杀**：超出 drain 预算的会话走执行脊柱既有取消传播（`cancelInFlight`）强杀当前轮次，再走正常会话关闭路径。
- **关闭时联动**：每个会话的关闭路径（`SessionResourceRegistry.closeAll()`）天然完成 `EXIT` 档 flush → 停心跳 → 关执行器 → **释放租约**——drain 只负责「让会话走到 close」，不重写 teardown 逻辑。
- **SmartLifecycle 衔接**：autoconfig 层注册 drain 生命周期 bean，停机动作清单 = 拒新 → 等在途/超时强杀 → （随会话 close）EXIT flush → 观测异步管线排空；超时预算与 `spring.lifecycle.timeout-per-shutdown-phase` 对齐；flush **同步执行**（虚拟线程 daemon 语义下 JVM 退出不等后台任务）。
- **可接管性天然供给**：不做显式跨实例迁移协议——drain 释放租约 + flush 状态后，调用方在实例 B 用同 `sessionId` 重新 `spawn()` 即续接（05 恢复语义保证历史完整）；「滚动发布会话续接模式」写入运维文档。

完整闭环：**停机信号 → SmartLifecycle 触发 drain → 拒新 spawn → 等在途轮次完结（或超时强杀）→ 会话 close（flush + 释租约）→ 观测管线排空 → JVM 退出；另一实例同 sessionId spawn 续接**。drain 全程事件进既有 observability 通道。

## User Stories

1. 作为平台集成者，我希望停机信号后 **runtime 拒绝新 spawn**（明确异常而非静默失败），这样我的路由层可以把新会话导向其他实例。
2. 作为平台集成者，我希望 drain **等在途会话的当前轮次完结**，这样进行中的工作不被掐断。
3. 作为平台集成者，我希望 drain 粒度是**轮次而非整个会话**，这样长会话不会无限阻塞发布。
4. 作为平台集成者，我希望 drain 有**超时预算**、超时后对残留会话**强杀当前轮次**（走既有取消传播），这样发布有确定的最长耗时。
5. 作为平台集成者，我希望强杀后会话仍走**正常关闭路径**（flush + 释放租约），这样被强杀会话与正常关闭会话的可接管性一致。
6. 作为平台集成者，我希望 drain 完成后**租约被释放**，这样实例 B 立即可以用同 sessionId spawn 续接，无需等 TTL 过期。
7. 作为平台集成者，我希望 **EXIT 档缓冲写在停机时被 flush**（复用 05 的 flush 钩子），这样选 EXIT 档的部署不因发布丢整轮。
8. 作为平台集成者，我希望 flush **同步执行**（不依赖后台虚拟线程），这样 JVM 退出不会跳过 flush。
9. 作为平台集成者，我希望 drain 经 **SmartLifecycle 挂进 Spring 关闭流程**，这样 `context.close()` / SIGTERM 自动触发，无需我手动调用。
10. 作为平台集成者，我希望 drain **超时预算与 `spring.lifecycle.timeout-per-shutdown-phase` 对齐**（默认派生、可覆盖），这样停机预算在 Boot 层面一处管理。
11. 作为 SRE，我希望 drain 全程**事件留痕**（drain 开始/会话等完/会话强杀/flush 完成/drain 结束，带计数与耗时），这样每次发布的处置过程可审计。
12. 作为 SRE，我希望**观测异步管线在 JVM 退出前排空**，这样 drain 事件本身不丢。
13. 作为 Agent 应用开发者，我希望**编程式 drain 入口**（不经 Spring 关闭也能触发 drain，如主动缩容/预热切换），这样非 Spring 场景同样可用。
14. 作为 Agent 应用开发者，我希望 drain 后**对已关闭会话的 chat() 调用得到明确异常**，这样行为可预期。
15. 作为 Agent 应用开发者，我希望机制 **safe-by-default**（autoconfig 下默认装配 drain 生命周期 bean），这样引入即受保护。
16. 作为平台集成者，我希望有**一键关闭 drain**（及调 drain 超时）的 yml 配置，这样特殊部署可回退。
17. 作为 Harness 集成者，我希望本机制作为 **core + autoconfig 扩展**（不新增模块），因为 drain 横跨会话台账、执行脊柱取消与存储 flush。
18. 作为 Harness 集成者，我希望**星形依赖不被破坏**（不新增 feature 模块耦合），这样 17 模块物理无环图不变。
19. 作为平台架构者，我知晓**不做显式跨实例迁移**（实例间 RPC 移交），因为「五 SPI + 租约 + 05 恢复语义」已天然供给可接管性。
20. 作为平台架构者，我希望「滚动发布会话续接模式」**写入运维文档**（drain → 释租约 → 实例 B 同 sessionId spawn），这样迁移伪需求有标准答案。
21. 作为 Agent 应用开发者，我希望**模型调用进行中**的轮次在超时强杀时被取消传播尽力中断（工具层可中断、模型层依赖底层超时），且该边界被文档化。
22. 作为 Agent 应用开发者，我希望公共 API 变更（drain 入口、拒新异常）**全部 additive**，这样既有集成源码/二进制兼容。
23. 作为平台集成者，我希望 drain 与 Boot graceful shutdown 的**相位关系明确**（先停收新流量还是先 drain 会话，文档化且可配），这样我能按部署拓扑排序。
24. 作为 SRE，我希望 drain 期间**会话事件通道继续工作**（drain 事件可被监听），这样排障不缺信息。
25. 作为平台集成者，我希望重复触发 drain **幂等**（二次 SIGTERM / 重复 stop 不炸），这样编排层重试安全。

## Implementation Decisions

### 改动面与定位

- **core 扩展 + autoconfig 装配，不新增模块**（06 决策票与路线图口径一致：「core + autoconfig 扩展」）。drain 横跨三处：runtime 会话台账与 drain 编排（`DefaultAgentRuntime`）、会话关闭路径（`DefaultAgentSession` / `SessionResourceRegistry`，复用不改语义）、执行脊柱取消传播（`HarnessToolCallingManager.cancelInFlight`，复用不改）。
- **不新增持久化 SPI**：EXIT 档 flush 复用 05 已交付的 `DurabilityTieredStores.flush` 钩子（已注册进每会话 `SessionResourceRegistry`，[11-crash-recovery.md](../docs/spec/11-crash-recovery.md) §「06 联动」预留位）；观测管线排空复用 `AsyncObservabilityPipeline` 既有 `close()`/shutdown hook。
- **公共 API 变更（全部 additive，PR 须说明兼容性）**：`AgentRuntime` 新增 drain 相关 default 方法（default 实现可为 no-op/抛 UnsupportedOperation 之外的温和兜底，保持二进制兼容）；新增 `RuntimeDrainingException`（或同义命名）拒新异常类型。既有集成不受影响。

### 会话台账（基础项）

- `DefaultAgentRuntime` 维护活跃会话注册表：`spawn()` 成功时注册，`DefaultAgentSession.close()` 时注销（经既有 `onClose` 回调链挂上，不新增会话生命周期切面）。
- 注册表用 `ConcurrentHashMap`（并发规约）；只存引用与最小元数据（sessionId、是否轮次在途），不复制会话状态。
- **轮次在途信号**：复用既有 `SessionObserver.onTurnStart/onTurnEnd`（或 hookEnv 轮次边界）维护「当前轮次进行中」标志——drain 等待的就是这个标志归零，不引入新的轮次追踪机制。

### drain 三步协议

- **入口（决策）**：runtime 级 `drain(Duration timeout)` 编程式入口 + SmartLifecycle 自动触发，两条路径走**同一编排实现**（SmartLifecycle 只是触发器）。drain 幂等：`AtomicBoolean`/状态机保证并发与重复触发只生效一次，后续调用等待首次 drain 结果。
- **① 拒新（决策）**：drain 开始即置拒新标志；其后 `spawn()` 抛 `RuntimeDrainingException`（携带 sessionId +「实例正在 drain」上下文，异常规约）。**不排队、不缓冲**——拒绝是调用方路由的信号。
- **② 等在途（决策）**：快照当前活跃会话，逐个等待「当前轮次完结」——轮次在途的会话等其 `onTurnEnd`/`onTurnError`；轮次完结后**主动 close 该会话**（走 `SessionResourceRegistry.closeAll()`：EXIT flush → 停心跳 → 关执行器 → 释租约）。未在轮次中的会话直接 close。等待有总预算（drain timeout）。
- **③ 超时强杀（决策）**：预算耗尽仍在轮次中的会话，调 `session.cancel()`（既有 `cancelInFlight` 取消传播）后 close。**边界（文档化）**：工具调用可中断（`future.cancel(true)`）；模型调用阶段无取消句柄（05 探查事实），强杀只能等模型层自身超时返回——此边界写进机制文档，不伪造能力。
- **等待实现**：虚拟线程 + `CountDownLatch`/`Phaser` 按会话计数（对齐 `HarnessToolCallingManager` 虚拟线程 fan-out 手法），**禁止**轮询 sleep；单会话等待失败不阻塞其他会话（收集异常、最后汇总，对齐 `SessionResourceRegistry.closeAll` 的首异常收集手法）。

### SmartLifecycle 衔接（autoconfig 层）

- 新增 drain 生命周期 bean（core autoconfig 内注册，`@ConditionalOnProperty("buzhou.shutdown.drain-enabled")` 默认开，safe-by-default）：`SmartLifecycle.stop()` 触发上述 `drain(timeout)`。
- **超时预算（决策）**：默认从 `spring.lifecycle.timeout-per-shutdown-phase` 派生（该属性本就约束每相位停机预算），`buzhou.shutdown.drain-timeout` 可覆盖；两者皆无则用保守默认（配置常量，禁魔法数字）。
- **相位（决策）**：drain bean 的 phase **先于**观测管线关闭（drain 事件需要管线还活着）、与 web 容器关闭相位的先后关系文档化 + 可配（默认让 web 先停收新流量再 drain 会话，或反之，由属性控制；推荐默认 = web graceful 相位之后，保证 drain 事件与 flush 不被 web 关闭干扰——实现期按 Boot 4 相位常量定值并在文档注明）。
- **观测管线排空**：drain 完成后、JVM 退出前，observability 异步管线经其既有 `close()`（shutdown hook）排空；drain bean 不重复实现排空，只保证**相位在其之前**。
- **flush 同步执行**：EXIT flush 本就注册在会话 `closeAll()` 路径上、由 drain 等待的线程同步触发——天然满足「不依赖后台虚拟线程」；不新增异步 flush。

### 事件进 observability

- 新增普通观测事件类型（非治理/审计族）：`drain-started`（活跃会话数）、`drain-session-completed`（sessionId + 等完/强杀 + 耗时）、`drain-timeout-force-kill`（sessionId 列表）、`drain-finished`（等完数/强杀数/总耗时）。flush 完成已由 05 的 `durability-tier`/会话关闭路径覆盖，不重复发。
- 事件经会话/runtime 既有事件通道（`DefaultAgentSession.emit` / hookEnv 事件发布器），**不新增 SPI**；runtime 级事件（drain-started/finished 无单会话归属）经 hookEnv 事件发布器直发。

### 配置范围

- `@ConfigurationProperties(prefix="buzhou.shutdown")`（record + boxed 类型、null=未配置，对齐 `BuzhouRecoveryProperties` 模板）：`enabled`（默认开）/ `drain-timeout`（默认 null=派生自 Boot 相位预算）/ 相位相关开关（如需）。
- 绑定级覆盖不在本期（同 03/05 M1 口径：policy 消费管线打通前只到默认 + yml 两层）。

### 工程纪律

- 不新增模块、不新增持久化 SPI；行为变更带测试（CONTRIBUTING 约定）。
- 公共 API 变更（`AgentRuntime` drain 入口、拒新异常）在 PR 描述说明兼容性影响（全 additive / default 方法）。
- 借鉴：Anthropic Managed Agents 的 harness 无状态化（`wake(sessionId)` 从持久化日志重建会话——显式迁移是伪需求的架构佐证）；Spring Boot 4 graceful shutdown + SmartLifecycle 相位模型（底座接缝）。一手链接见 `docs/production-readiness/references.md` 06 条目。

## Testing Decisions

### 什么是好测试

只测**外部行为**——drain 调用的返回值/异常、spawn 的拒绝行为、工具调用计数器、会话事件流、store 中最终落盘的消息——不测会话注册表内部结构、状态机私有字段。「拒新是否生效」「轮次是否被等完还是强杀」「EXIT 缓冲是否落盘」一律从外部观察判定（同 `CrashRecoveryEndToEndTest` / `AgentSessionSpineTest` 的 e2e 哲学）。

### 缝合点（已与 owner 确认：2 个，全部复用既有缝合点形态，不新增测试基础设施）

1. **主缝合点（最高、复用既有 e2e 形态）——端到端 drain 测试**：
   - 复用 `CrashRecoveryEndToEndTest` 的装配与故障注入手法：`Buzhou.runtime(model, stores, ...)` + core test-jar 的 `ScriptedChatModel` + `CountDownLatch` 阻塞工具（带调用计数器）+ 短 TTL 租约。
   - 经真实 runtime 驱动新增的 drain 入口，断言：
     ① drain 开始后 `spawn()` 抛拒新异常、异常携带上下文；
     ② 在途轮次（阻塞工具持有 latch）被**等完**——释放 latch 后轮次正常完结、drain 随后返回、工具计数器符合预期；
     ③ **超时强杀**——不释放 latch、drain 超时后取消传播生效（工具收到中断语义）、会话仍被 close（租约已释放：同 sessionId 立即可再 spawn）；
     ④ **EXIT 档联动**——`durability-tier=EXIT` 下 drain 关闭会话后，缓冲消息在 store 中可见（flush 已同步执行）；
     ⑤ **事件流**——`drain-started` / `drain-session-completed` / `drain-timeout-force-kill` / `drain-finished` 按序、计数正确；
     ⑥ **幂等**——并发/重复 drain 只生效一次，后续调用得到同一结果。
   - **确定性**：全部用 `CountDownLatch` / 有界轮询（`LEASE_POLL_MILLIS` 手法），**不用** wall-clock sleep。
   - 先验：`CrashRecoveryEndToEndTest`（故障注入 + latch 阻塞工具 + 双实例）、`AgentSessionSpineTest`（会话脊柱 e2e）、`HarnessToolCallingManagerTest`（取消传播手法）。

2. **装配缝合点（复用既有 autoconfig 测试形态）——SmartLifecycle 注册与配置测试**：
   - 复用 `BuzhouCoreAutoConfigurationTest` 的 `ApplicationContextRunner` 形态：`ScriptedChatModel` 作 ChatModel bean，断言——
     ① 默认装配 drain 生命周期 bean（safe-by-default）、`buzhou.shutdown.enabled=false` 时不装配；
     ② `buzhou.shutdown.drain-timeout` 绑定生效、缺省时派生逻辑正确（ Boot 相位预算存在时取之）；
     ③ `context.close()`（或生命周期 stop）触发 drain：预先 spawn 的会话被 close、拒新标志生效——经上下文关闭这一**最高装配缝合点**验证 Spring 衔接，不 mock SmartLifecycle  internals。
   - 先验：`BuzhouCoreAutoConfigurationTest`（ApplicationContextRunner + test-jar ScriptedChatModel）。

### 被测模块

- `buzhou-core`（主：runtime 台账 + drain 编排 + 会话关闭路径 + autoconfig 生命周期 bean）。
- 不涉及新模块；store 无语义变更（EXIT flush 钩子 05 已交付并被契约测试锁定）；observability 模块不改（只要求相位关系）。

## Out of Scope

- **显式跨实例会话迁移协议**（实例间 RPC 移交）——06 决策票明确不做；可接管性由五 SPI + 租约 + 05 恢复语义天然供给。
- **模型调用阶段的强制中断**——现状无取消句柄（工具层可中断、模型层等自身超时），本机制只文档化边界，不伪造能力；如需模型流中断归后续韧性层工作。
- **Boot graceful shutdown 本身的 web 请求生命周期管理**——底座职责，本机制只做相位衔接。
- **drain 期间的会话级状态快照/增量 checkpoint**——消息即检查点（05 口径），不新增快照机制。
- **多实例编排层的滚动发布策略**（K8s rollout、分批、健康检查联动）——部署层职责；本机制只交付单实例 drain 原语 + 续接模式文档。
- **07 背压限速、08 死循环检测**——M1 同族但独立机制，各自立项。
- **15 运行时干预（挂起-回填）**——M2；drain 的「等轮次完结」与干预的「挂起」语义不同，不提前统一。
- **绑定级 policy 消费**——同 03/05 M1 口径，待 policy 消费管线打通。

## Further Notes

- **管辖 ADR**：wayfinder「production-readiness」06 号票（决策=做 drain 协议、粒度=当前轮次、不做显式迁移、SmartLifecycle 四步清单）+ 02 号票（底座事实：Boot graceful 只管 web 请求、虚拟线程 daemon 语义）。路线图落点：`docs/production-readiness/README.md` M1 行「06 优雅停机与会话 drain | 做 | core + autoconfig 扩展」。
- **与 05 的接口清单（均已交付，直接消费）**：`DurabilityTieredStores.flush`（EXIT 档 flush 钩子，已注册进 `SessionResourceRegistry`）；`LeaseHeartbeat`（drain close 会话时经 `closeAll()` 逆序停止）；`RecoveryConfig`/`BuzhouRecoveryProperties` 配置模板范式；`CrashRecoveryEndToEndTest` 测试骨架。
- **实现期须核验的开放项**：
  1. **SmartLifecycle 相位定值**：Boot 4 中 web 容器关闭相位 vs 默认相位的具体常量，及观测管线关闭的相对顺序——实现期按 Boot 4 源码定值并在机制文档注明；若观测管线不经 SmartLifecycle 而是 shutdown hook，需在文档说明「drain 事件先于管线排空」的保证依据。
  2. **轮次在途信号的精确来源**：`SessionObserver.onTurnStart/onTurnEnd` 是否覆盖全部轮次形态（chat/stream/自动续跑 AUTO_RESUME 轮次）——实现期验证，确保 drain 等待不漏不错。
  3. **拒新与租约 steal 的交互**：drain 中另一实例对同 sessionId 发起 `spawn(steal)` 属正常续接路径——拒新只拒绝**本实例**的新 spawn，不影响其他实例接管；实现期确认异常类型不向 steal 场景误发。
  4. **`AgentRuntime` 接口扩展形态**：default 方法的具体签名（同步阻塞 `drain(Duration)` vs 返回句柄）以实现期最小语义面定，遵守「api 子包 SPI 禁止破坏性签名变更」规约。
- **未来集成点（设计时预留，不在本期实现）**：15 运行时干预平面可复用 drain 的「轮次边界等待」原语；21 多租户的按租户 drain（灰度发布单租户下实例）；07 背压与 drain 拒新共用「拒新即路由信号」语义。
- **运维文档交付**：「滚动发布会话续接模式」（drain → 释租约 → 实例 B 同 sessionId spawn → 05 恢复语义加载历史）写入机制文档运维章节，作为「不做显式迁移」的标准答案。
