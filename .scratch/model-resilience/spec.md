# Spec: 模型韧性层（M1 — 错误归一化分类 + 重试 + 统一超时）

Status: M1 已落地（01–06 全部 done）→ 正式机制详设见 [docs/spec/10-resilience.md](../../docs/spec/10-resilience.md)
源决策: wayfinder「production-readiness」03 号票（grilling 收口，决策=做，分 M1/M2 两期）+ 02 号票（Spring AI 2.0 底座留白盘点）
里程碑: M1 稳定基线（路线图点名「03 M1 先行」）

---

## Problem Statement

让单个业务 Agent 稳定地跑在生产里，模型调用这一跳目前是**完全无保护**的：

- **没有重试**：provider 限流（429）、偶发 5xx、瞬时网络抖动，任何一次都直接让当轮失败。Spring AI 2.0 底座的重试是全局单例且 **429 默认不重试**（02 号票盘点结论），不满足生产预期。
- **没有统一超时**：模型调用在调用线程上同步执行、无 deadline，provider 卡死或网络挂起会无限挂住整个会话。底座无统一超时属性（02 号票）。
- **错误不可观测、口径不一**：不同 provider 的异常形态各异（HTTP 状态码、非透明运行时异常、甚至「不抛异常只回元数据」的内容拒绝），上层无法用统一口径统计失败、设告警、做治理决策。
- **没有给护栏/观测 Hook 感知错误的切面**：现有 Hook 链只有 beforeModel/afterModel，模型调用失败时护栏与观测逻辑无从介入（无法兜底、无法改写、无法归因留痕）。

从用户视角：业务 Agent 作者只想拿到一次可靠的模型回复，却要自己手写重试循环、猜各 provider 的异常、拼超时；SRE 想看清「模型到底为什么在失败、重试了多少次」却没有统一口径的事件。

## Solution

不周山自研一层**模型韧性层（Model Resilience）**，挂在 Spring AI 的 ChatClient Advisor 链上（叠加而非替代），把「对模型调用的重试 / 超时 / 错误归一化」收归框架：

- 一个 **ResilienceAdvisor** 作为最内层模型包裹 Advisor，每次模型调用都流经它：在内完成「错误归一化分类 → 按策略重试 → 统一超时」。
- 一套跨 provider 的**归一化错误分类**（限流 / 鉴权 / 内容 / 网络 / 未知），框架内置常见 provider 默认分类器，`ProviderErrorClassifier` SPI 留给非标 provider 扩展。
- Hook 链新增 **onModelError** 切面，让护栏 / 观测 Hook 能感知失败、可吞错兜底。
- 重试、超时、分类事件全部进 observability 事件流，给 SRE 统一口径。
- 参数走 policy 多层配置（默认 < yml），safe-by-default、引入即生效、可一键关。

本 Spec 只覆盖 **M1**：错误归一化分类 + 重试（含 Retry-After）+ 统一超时 + onModelError 切面 + 可观测事件。**M2（熔断 + 兜底降级 + 多模型路由/降级链）另开 Spec**，与 04 号票同炉设计、复用 M1 的同一个 ResilienceAdvisor 执行点。

## User Stories

1. 作为 Agent 应用开发者，我希望模型调用默认带合理重试与超时，这样我引入 buzhou-resilience 即得生产级韧性、无需手写重试循环。
2. 作为 Agent 应用开发者，我希望该机制 safe-by-default（引入即生效、可一键关），这样不增加接入负担。
3. 作为 Agent 应用开发者，我希望遇到 429（限流）时自动按退避重试，这样短时限流不会让单次会话失败。
4. 作为 Agent 应用开发者，我希望重试尊重 `Retry-After` 头，这样不会因过早重试加重对 provider 的压力。
5. 作为 Agent 应用开发者，我希望瞬时网络错误（连接重置、读超时等）自动重试，这样 provider 抖动对用户不可见。
6. 作为 Agent 应用开发者，我希望 5xx 服务端错误按策略重试，这样 provider 偶发故障可自愈。
7. 作为 Agent 应用开发者，我希望重试带抖动（jitter），这样多实例同时重试不会形成惊群。
8. 作为 Agent 应用开发者，我希望重试次数有上限（max-attempts），这样不可恢复的故障不会无限重试耗尽资源。
9. 作为 Agent 应用开发者，我希望鉴权类错误（401/403/无效 key）不重试、快速失败，这样配置错误不被重试掩盖、能尽快暴露。
10. 作为 Agent 应用开发者，我希望未知错误默认不重试（保守、可预期），并允许我配置将未知纳入重试。
11. 作为 Agent 应用开发者，我希望**内容拒绝**（provider 内容过滤的静默拒绝）被识别为单独类别、不被当作可重试错误反复重试。
12. 作为 Agent 应用开发者，我希望模型调用有统一超时（deadline），这样卡死的调用不会无限挂住会话。
13. 作为 Agent 应用开发者，我希望超时触发后调用被取消（中断传播进在途请求），这样不留下僵尸在途调用。
14. 作为 Agent 应用开发者，我希望 `session.cancel()`（运行时干预）也能中断在途的模型调用——当前它只能中断工具调用，模型调用是漏网之鱼。
15. 作为平台集成者，我希望 deadline 可按模型 / 绑定级配置，这样不同 provider / 模型可设不同时限。
16. 作为 SRE，我希望每次重试都进 observability 事件流（带类别、次数、退避时长），这样我能看到重试风暴并设告警阈值。
17. 作为 SRE，我希望超时与最终失败进事件流，这样排障时能还原模型调用失败现场。
18. 作为 SRE，我希望错误按五类（限流 / 鉴权 / 内容 / 网络 / 未知）归一化上报，这样跨 provider 的统计口径一致。
19. 作为 SRE，我希望内容拒绝被单独标记上报（即使不抛异常），这样内容安全治理可见、可统计。
20. 作为平台集成者，我希望有一个 onModelError Hook 切面，这样我能在模型调用失败时自定义兜底 / 改写 / 上报。
21. 作为平台集成者，我希望 onModelError 允许**吞错并返回兜底响应**，这样终端用户在模型不可用时仍能得到受控回复而非裸异常。
22. 作为平台集成者，我希望 onModelError 允许**放行**（让异常按原语义抛出），这样不接入兜底时行为与底座一致。
23. 作为平台集成者，我希望重试 / 退避 / 超时参数可经 yml 配置（`buzhou.resilience.*`），这样无需改代码即可调参。
24. 作为平台集成者，我希望参数走 policy 多层模型（默认 < yml），这样既有合理默认、又能按部署覆盖。
25. 作为平台集成者，我希望未来能按**绑定级**覆盖韧性参数（mechanismOverrides），这样不同 Agent 可有不同韧性档位（M1 地板：默认 + yml）。
26. 作为 Agent 应用开发者，我希望可以一键关闭整个韧性机制（`buzhou.resilience.enabled=false`），这样排查时可回退到底座原生行为。
27. 作为分类器扩展者，我希望有 `ProviderErrorClassifier` SPI，这样接入新 provider 时能补它的「异常 → 类别」映射。
28. 作为分类器扩展者，我希望框架内置 OpenAI / Anthropic / RestClient 系默认分类器，这样常见 provider 开箱即用、只有非标 provider 才需扩展。
29. 作为平台集成者，我希望引入 buzhou-resilience 后底座 Spring AI 的内置重试被调小 / 避让（并写入机制文档），这样不会与不周山重试叠加成双倍重试。
30. 作为 Harness 集成者，我希望 buzhou-resilience 是独立可用模块（只依赖 buzhou-core），这样我可以只引这一个模块得到模型韧性、不被迫引入其它机制。
31. 作为 Harness 集成者，我希望该机制遵守星形依赖（feature 模块互不依赖、跨机制协作走事件总线），这样不破坏既有 16 模块物理无环依赖图。
32. 作为 Harness 集成者，我希望该机制**不引入** Resilience4j / Spring Retry 等新运行时依赖，这样依赖面保持精简、与 03 决策一致。
33. 作为 Agent 应用开发者，我希望流式调用（`.stream()`）也有超时与错误分类，这样流式场景同样受保护。
34. 作为 Agent 应用开发者，我理解 M1 不对流式已发出的 token 做中途重试（该边界不可行），这样我对其行为有正确预期。
35. 作为 Harness 集成者，我希望该模块有自己的 AutoConfiguration + `AutoConfiguration.imports` 注册 + `@ConditionalOnProperty` 开关，这样遵循仓库 starter 装配约定。
36. 作为 Harness 集成者，我希望该模块被 `buzhou-spring-boot-starter` 聚合、版本进 `buzhou-bom`，这样引入 starter 即得。
37. 作为平台架构者，我希望 M1 的 ResilienceAdvisor 为 M2（熔断 / 降级链 / 路由）预留同炉扩展位，这样 M2 不必重写执行点。
38. 作为平台架构者，我希望 onModelError 切面与 07 限流 Advisor、08 转人工出口共享同一 Advisor / Hook 平台，这样三机制可协同（如重试耗尽 → 转人工）。

## Implementation Decisions

### 模块与改动面

- **新增 feature 模块 `buzhou-resilience`**：独立可用、仅依赖 `buzhou-core`，遵守星形依赖白名单（不与 memory/spill/guard/tools/store-* 等互依）。结构对齐 `buzhou-observability`（因为它是「贡献 Advisor」而非「贡献 Hook」的模块，比 `buzhou-guard` 更接近）：自带 pom（buzhou-core + jackson-databind + spring-boot-autoconfigure）、包根 `io.github.chyuan_cuihongyuan.buzhou.resilience`、自有 `BuzhouResilienceAutoConfiguration`（`@ConditionalOnProperty(prefix="buzhou.resilience", name="enabled", matchIfMissing=true)`，safe-by-default 默认开）、经 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。
- **`buzhou-bom` 新增版本条目**、**`buzhou-spring-boot-starter` 新增聚合依赖**（starter 只聚合、无代码，既有约定）。
- **core 扩展（公共 API 变更，需 PR 说明兼容性）**：`BuzhouHook` 新增 `onModelError` 切面。以 **default 方法** 形式加入（默认 no-op），对既有 Hook 实现者源码兼容、二进制兼容。

### 执行点：ResilienceAdvisor

- **形态**：一个 `BaseAdvisor`，经 `SessionAssemblyCustomizer` + `SessionAssemblyContext.addAdvisor(...)` 注入 ChatClient 链（对齐 `ObservabilityModule` 的既有 Advisor 注入模式；这是仓库里贡献 Advisor 的唯一既定通道）。
- **位置（决策）**：**最内层模型包裹 Advisor**，紧邻 `HookAdvisor`（与 03 决策「与 HookAdvisor 相邻挂 ChatClient 链」一致）。语义目的是让重试 / 超时作用于**单次模型调用**（落在 ToolCallingAdvisor 的工具循环内部、每次模型调用粒度），而非整个轮次——这样工具循环中途的一次 429 只重试那一次模型调用，不会重启整轮工具链。
- **与 Hook 的相对关系（决策）**：ResilienceAdvisor 在 HookAdvisor 之内，因此 `beforeModel`/`afterModel` 观察到的是「经韧性层解决后」的一次逻辑模型调用（重试对它们基本不可见，或仅以事件形式可见）；`onModelError` 在韧性层**重试耗尽 / 命中不可重试 / 超时**之后、决定兜底或放行之前触发。
- **待实现期核验的开放项**：当前 `HarnessAssembler` 把 customizer 注入的 Advisor 追加在三个核心 Advisor 之后；Spring AI 实际按 `getOrder()` 排序而非按列表位置，因此 ResilienceAdvisor 需用**显式 order 值**落到最内层目标位（紧邻 HookAdvisor）。实现期须验证该 order 真的让它包裹的是裸 ChatModel 调用，并在机制文档里固定下来。

### 归一化错误分类 + ProviderErrorClassifier SPI

- **五分类**（与 CONTEXT.md 术语一致）：限流 / 鉴权 / 内容 / 网络 / 未知。
- **默认可重试判定**（决策表）：

  | 类别 | 触发示例 | M1 默认是否重试 |
  |---|---|---|
  | 限流 | 429、Retry-After | 是 |
  | 网络 | 连接重置、读超时、瞬断 | 是 |
  | 鉴权 | 401/403、无效 key | 否 |
  | 内容 | 内容拒绝（finishReason=CONTENT_FILTER 等元数据标记，**不抛异常**） | 否 |
  | 未知 | 不可识别的异常 | 否（保守，可配置为重试） |

- **`ProviderErrorClassifier` SPI**：把「异常 + 响应元数据 → 类别」的映射做成可扩展点；框架内置 OpenAI / Anthropic / RestClient 系默认分类器，非标 provider 走 SPI 补。
- **内容拒绝是静默通道**：不抛异常、仅元数据，分类器须从响应里识别并归入「内容」类。本层只负责**分类 + 保证 afterModel / onModelError 可观测**，**治理策略归 12 号票内容安全机制**（本层不做内容拒绝的拦截 / 改写策略）。

### 重试策略

- 指数退避 + 抖动；尊重 `Retry-After`（钳制到 max-backoff 上限）；可配 max-attempts / initial-backoff / max-backoff / multiplier / jitter。
- 借鉴 LangChain v1 `ModelRetryMiddleware`（容错下沉为中间件）+ LangGraph `RetryPolicy`（声明式重试、默认值含抖动）的形态；**不引依赖**，手写小回路。

### 统一超时（deadline）

- 模型级 deadline：把模型调用包进虚拟线程任务、用 `Future.get(deadline)` 兜底（**对齐执行脊柱 `HarnessToolCallingManager` 既有的工具超时手法**，复用同一思路而非另造）；超时 `cancel(true)` 把中断传播进在途模型调用。
- **补 session.cancel() 的缺口（决策）**：当前 `session.cancel()` 只中断工具、不中断模型调用（02 号票与代码盘点确认的漏网）。M1 让运行时干预也能中断在途模型调用——deadline 与 cancel 共用同一条中断传播路径。
- 默认 deadline 值取一个生产合理值，可经 yml 覆盖、未来按绑定级覆盖。

### onModelError 切面

- 触发时机：韧性层判定**终态失败**之后（重试耗尽 / 不可重试类别 / 超时）、在「兜底或放行」之前。
- 语义：允许 Hook **吞错并返回兜底响应**（复用既有 `HookResult.Replace` / beforeModel-Block 回填响应的同构能力），或**放行**让异常按底座原语义抛出。借鉴 Google ADK `on_model_error_callback`（错误回调吞错兜底 / 改写控制流）。

### 与底座的叠加避让

- 语义分工：底座 Spring AI 只管网络瞬断；429 / 5xx / 超时 / （M2）熔断 / 降级 归不周山。
- 数值避让：AutoConfiguration 给底座 `spring.ai.retry` max-attempts 一个被调小的建议默认（或文档强约束），避免双倍重试；写入机制文档。

### 事件进 observability

- 新增事件类型（普通观测事件，**非**治理 / 审计族——那是 M2/M3）：`retry-attempted`（带类别 / 次数 / 退避）、`retry-exhausted`、`timeout-fired`、`error-classified`（带五类标签）、`content-refusal-detected`。
- 走会话既有事件通道（与 Hook 事件同炉），**不新增 SPI**。

### 配置范围（M1 地板）

- M1 实现 policy 的 **默认 + yml** 两层：`@ConfigurationProperties(prefix="buzhou.resilience")` record（boxed 类型、null=未配置，对齐 `SpillProperties` 模板）：`enabled` / `max-attempts` / `initial-backoff` / `max-backoff` / `multiplier` / `jitter` / `deadline` / `retryable-categories`（覆盖默认可重试集合）。
- **绑定级层（mechanismOverrides）移出 M1**：该层存储已就位、但「被下游机制消费」的管线尚未接通（盘点确认 LayeredPolicy 脚手架未连装配、BindingPolicy.mechanismOverrides 仅在测试中断言）。M1 不半接这根线；绑定级覆盖作为后续前置项（policy 消费管线打通后）再纳入。

### 工程纪律

- 不引入 Resilience4j / Spring Retry（与 03 决策一致）。
- 行为变更带测试（CONTRIBUTING 约定）；store 语义不涉及（无契约测试扩展）。
- 公共 API 变更（`BuzhouHook.onModelError`）在 PR 描述说明兼容性影响（default 方法，兼容）。

## Testing Decisions

### 什么是好测试

只测**外部行为**（最终回复 + observability 事件流），不测重试循环的内部状态 / 私有字段。模型的内部决策（重试了几次、按什么类别处理、是否超时）一律通过「事件流里出现了什么事件」与「最终回复是什么」从外部观察判定——这是仓库既有的 e2e 测试哲学。

### 缝合点（已与 owner 确认：2 个）

1. **主缝合点（最高、复用既有模式）——端到端经 `AgentRuntime.spawn().chat()` + 可注入故障的 ChatModel**：
   - 扩展 core test-jar 里的 `ScriptedChatModel`，使其可被脚本化「第 1..n 次调用抛异常 X（带 Retry-After / 内容拒绝元数据），第 n+1 次返回 Y」；复用 `HookEndToEndTest` 既有的「包一层 observing ChatModel」手法补「抛错」能力。
   - 经真实的 `Buzhou.runtime(...)` 装配出完整 Advisor 链（含 ResilienceAdvisor）后 `spawn().chat()`，断言**最终回复**与 **observability 事件流**。
   - 该缝合点覆盖：五类错误分类、重试 + 退避、Retry-After 尊重、超时、内容拒绝透传、onModelError 兜底 / 放行、事件上报。
   - **超时确定性**：用 `CountDownLatch` 阻塞的慢模型 + 短 deadline，断言在预算内返回超时事件（对齐 `HarnessToolCallingManagerTest` 的超时手法，**不用** wall-clock sleep）。
   - **流式**：同缝合点为 `.stream()` 单独覆盖「deadline + 分类 + onModelError」，并**断言不发生中途重试**（单次失败即传播）。
   - 先验：`HookEndToEndTest`、`AgentSessionSpineTest`、`HitlGuardEndToEndTest`（同一 e2e 形态）、`HarnessToolCallingManagerTest`（超时 / cancelInFlight 手法）。

2. **次缝合点——`ProviderErrorClassifier` 纯函数表测试**：
   - 「异常 + 元数据 → 类别」是决策表，按 provider 穷举枚举（每个 provider 的每种异常形态 → 期望类别 + 是否可重试）。
   - 纯函数、无链路、廉价，覆盖比经 e2e 枚举更清晰。
   - 先验：无直接同类（新表测试），形态上类比契约测试 `AbstractBuzhouStoresContractTest`「穷举 SPI 表面」的思路。

### 被测模块

- `buzhou-resilience`（主）+ `buzhou-core` 的 `BuzhouHook.onModelError` 扩展（次）。
- 不涉及 store SPI，无需扩展契约测试。

## Out of Scope

- **M2 全部**：熔断（CLOSED/OPEN/HALF_OPEN 状态机）、自动兜底降级、多模型路由 / 降级链 / failover / 模型档位组 / 调用级路由（04）——复用 M1 同一 ResilienceAdvisor，另开 Spec。
- **网关层 QPS / 分布式限流**（归 07）。
- **内容拒绝的治理策略**（拦截 / 改写 / 重试带原因）——归 12 内容安全；本层只分类 + 可观测。
- **工具调用容错 / 重试 / 结果缓存 / 工具熔断**（归 09，工具层，同族状态机但作用域不同）。
- **成本治理**（11）、**死循环与失控检测**（08）——不同家族。
- **崩溃中轮次恢复 / 幂等**（05）——模型重试是只读调用、无幂等顾虑；崩溃恢复是独立家族。
- **policy 的绑定级层与 per-tool 层消费**：M1 只到默认 + yml；绑定级等 policy 消费管线打通后再纳入。
- **多租户韧性配额**（归 11/21）。
- **流式已发 token 的中途重试**（边界不可行，M1 不做）。
- **Resilience4j / Spring Retry 集成**（明确不引）。

## Further Notes

- **管辖 ADR**：wayfinder「production-readiness」03 号票（决策=做、M1/M2 分期、执行点=ResilienceAdvisor、错误感知=Hook onModelError 切面、事件进 observability）+ 02 号票（底座 429 默认不重试、无统一超时、无熔断/fallback——不周山合法空间）。本 Spec 落地 03 的 **M1** 子集。
- **借鉴清单**（一手链接见 `docs/production-readiness/references.md` 的 03 条目）：
  - LangChain v1 `ModelRetryMiddleware` / `ModelFallbackMiddleware`（容错下沉为中间件）
  - LangGraph `RetryPolicy`（声明式重试、默认值含抖动）
  - Google ADK `on_model_error_callback`（错误回调吞错兜底 / 改写控制流 → onModelError 蓝本）
  - OpenAI Agents SDK `error_handlers`（终态错误 → 受控输出）
  - Resilience4j 熔断状态机语义（**仅借概念、M2 用**，不引依赖）
  - 底座事实依据：02 号票研究（`research/spring-ai-baseline` 分支）
- **模块时序**：路线图点名「03 M1 先行」；本模块是 M1 稳定基线的第一个。后续 05/06 依赖恢复语义与 SPI 现状梳理，07/08 可并行。
- **未来集成点（设计时预留，不在 M1 实现）**：ResilienceAdvisor 是 07「每模型 RPM/TPM 双桶 Advisor」与 M2「04 降级链」的同炉宿主；`onModelError` 是 08「重试耗尽 → 转人工出口」的天然挂载点。M1 的执行点与切面为这些预留位、M2 不必重写。
- **实现期须核验的开放项**：ResilienceAdvisor 经 `getOrder()` 落到「最内层、紧邻 HookAdvisor」的真实生效位（见 Implementation Decisions「执行点」），并在机制文档固定。
