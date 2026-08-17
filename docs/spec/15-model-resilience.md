# Spec 15 — 模型韧性与失控防护（mechanism）

> effort #4（impl-44/45）落地；机制内容自 `Future-needs-to-be-supplemented` 分支 spec（10-resilience /
> 13-backpressure-rate-limit / 14-runaway-detection）修订并入，编号避开 main 既有 10-14。
> 需求与验收上下文见 [spec 14 §A](14-perimeter-hardening.md)。

## 模型韧性层（buzhou-resilience）

- **ResilienceAdvisor**（advisor 链 order = ToolCallingAdvisor.DEFAULT_ORDER + 700，最内层模型包裹）：
  单次模型调用错误归一化分类 → 按策略重试。重试不重放外层 advisor（Hook 观察到的是「一次逻辑调用」）。
- **错误五类**（SRE 统一口径，`ErrorCategory`）：RATE_LIMIT（429，尊重 Retry-After 并钳制到 maxBackoff）/
  NETWORK（瞬时网络与 5xx——SERVER 不单独成类，impl 期并入 NETWORK，2026-08-17 回写）/ CONTENT（静默内容拒绝，不重试仅上报）/ AUTH（不重试）/ UNKNOWN。
  分类 SPI：`ProviderErrorClassifier`（默认实现识别 RestClient 系 HTTP 异常）。
- **指数退避**：`initial × multiplier^(attempt-1)`，钳制 `maxBackoff`，jitter `[0,1]` 打散惊群。
- **deadline**：单次模型调用统一超时（默认 60s；0 关闭）。执行经虚拟线程 executor +
  `Future.get(deadline)`，超时 `cancel(true)` 中断在飞调用；在飞注册 `ModelCallInFlight`，
  `session.cancel()` 同路径中断。流式：deadline 语义 = 首 token / 帧间空闲超时（`Flux.timeout`）。
- **onModelError 切面**（core）：终态失败（重试耗尽/不可重试/超时）后 `HookAdvisor` 触发
  `BuzhouHook.onModelError(ModelCallContext)`——默认放行（异常原语义上抛），
  `Replace(ChatClientResponse)` 吞错回填兜底、`Block(reason)` 回填文本；失败原因经
  `ModelCallContext.error()`。
- **限流**（`RateLimitAdvisor`，order +650 外于韧性层）：RPM 预检扣减 + TPM 预检（事后按 usage 记账）。
  过载两档 `OverloadPolicy`：QUEUE（有界排队 + 超时，默认）/ FAIL_FAST。自限流拒绝
  （`ModelRateLimitExceededException`）不进重试分类（防重试放大拥塞），直上 onModelError。
- **运维面**：`ResilienceStats`（重试/耗尽/限流拒绝/超时/最近分类 + `BuzhouHealth` 委托）、
  指标族 `buzhou.resilience.*`（core MeterBinder 预注册）、日志基线（重试 WARNING/耗尽 ERROR/
  限流拒绝 INFO）、配置 fail-fast（deadline < maxBackoff 等矛盾启动失败）。
- **熔断器（T81 / impl-56）**：手写 CB（不引 resilience4j），`ModelCircuitBreaker` 进程级注册表按
  modelName 分桶（`ResilienceModule.configure()` 创建一次、customizer 闭包注入全部会话）。
  **三态结果计数**（对齐 resilience4j 语义）：FAILURE = 终态失败且 category ∈ `circuit.failure-categories`
  （默认 `[NETWORK, SERVER, TIMEOUT]`）；RATE_LIMIT（背压归限流器）/ CONTENT（内容治理）/ AUTH（配置错误
  不遮蔽）/ UNKNOWN（不明不盲跳）为 IGNORED 不进窗口；成功为 SUCCESS。**状态机**：计数窗口
  （`window-size` 默认 20）失败率 ≥ `failure-rate-threshold`（默认 0.5）且样本 ≥ `min-calls`（默认 5）→
  OPEN；OPEN 期调用抛 `ModelCircuitOpenException`（不进重试分类，直上 onModelError，与自限流拒绝同语义）；
  冷却 `open-cooldown`（默认 30s）后放行**单探测**（probeInFlight 原子占位，并发拒绝）进 HALF_OPEN——
  探测 FAILURE 回 OPEN（重计冷却），SUCCESS/IGNORED 回 CLOSED（窗口重置）。每次**逻辑调用**记一次结果
  （重试 attempt 不重复记）；流式入口检查 + 终态记录。事件 `circuit.state-changed` / `circuit.call-rejected`
  走当次调用会话通道；指标 `buzhou.resilience.circuit-rejected` / `circuit-tripped` / `circuit-open`(gauge)；
  ResilienceStats 计数 + details states（有界）；机制健康恒 UP（宕的是 provider 不是机制，严格 DOWN 语义
  不误报）。配置 `buzhou.resilience.circuit.*` 默认启用，fail-fast 校验（window≥2、1≤min-calls≤window、
  0<threshold≤1、cooldown>0）。
- **备模型降级链（T82 / impl-57）**：主模型终态失败后在**同一逻辑调用**内降级（ResilienceAdvisor 最内层，
  外层 Hook/Memory 观察到一次成功调用——降级仅事件可见）。配置 `buzhou.resilience.fallback.models`
  （备模型 bean 名有序列表，Spring 按名解析、未命中 fail-fast；编程式 `NamedFallbackModel` 列表）。
  **触发**：主模型终态失败且 category ∈ `fallback.trigger-categories`（默认 `[NETWORK, SERVER, TIMEOUT,
  AUTH]`；CONTENT 不触发防策略跳舱）或**主模型熔断 OPEN 恒触发**（CB+降级：主断路打开后请求零重试直达
  备模型）。**无粘性**：每逻辑调用先主后备（OPEN 时 fail-fast 成本≈0），主模型半开探测成功自动回归。
  备模型各一次尝试（重预算已在主模型耗尽）、复用 deadline、熔断独立分桶记账（备 OPEN 则跳过该级）；
  全败**上抛主模型原始错误**（根因不遮蔽）。事件 `fallback.switched / fallback.exhausted`；指标
  `buzhou.resilience.fallback-switches / fallback-exhausted`；ResilienceStats 两计数。
  **流式边界**：M1 流式不降级（同「不做中途重试」边界），仍走 onModelError 静态兜底。
- **重试预算**：开放问题，未做（熔断已覆盖「持续故障停止锤 provider」；重试风暴防放大归限流器邻域）。

## 失控检测（core/runaway）

- **RunawayHook**（挂 Hook 链）：单轮行为失控的数值闸门，safe-by-default（阈值默认 null = 不限）。
- **四层硬顶**：单轮步数（`per-turn.max-steps`）/ 单轮工具调用总数 / 单轮墙钟（与 TurnDeadline 独立，
  先到先停）/ 会话累计双窗口（steps + tool-calls，SessionStateStore 持久化跨崩溃保留）。
- **软退出通道**：达硬顶 80% 软阈值时经 `RunawayBudgetRenderer`（AttachmentRenderer）注入
  剩余预算提醒——模型先自我收敛，而非猝死。
- **确定性重复检测**：同参数工具调用指纹连续重复即拦（`runaway.repetition` 事件）。
- 终态语义：硬顶 `HookResult.block`（携带部分结果指针——被终止 ≠ 前功尽弃）；
  事件 `runaway.soft-threshold / hard-stop / per-tool-exceeded / repetition` 双写
  （SessionEvent + ObservabilityStore EventRecord，dashboard 可查）。

## 会话容量闸（core/backpressure）

- **SpawnGate**：实例级并发活跃会话上限（`buzhou.backpressure.max-concurrent-sessions`，未配置不限）。
  裁决在**租约之前**（排队不持租约）；空位由会话 close 归还；steal 接管路径不占新容量。
- QUEUE 档：信号量公平排队 + `spawn-queue-timeout` 超时拒绝；drain/停机置位时唤醒全部等待者
  （抛 `SHUTDOWN_INTERRUPTED` 结构化异常，与 main 停机拒新语义同型）。
- 拒绝事件：`backpressure.spawn-queued / spawn-rejected`（reason：timeout/fail-fast/drain）；
  指标 `buzhou.backpressure.spawn-rejected`。

## 与既有机制的预算合成

- TurnDeadline（impl-29 绝对时刻）优先硬停；runaway 墙钟/步数先行软退出——两层互补不打架。
- 模型限流（本 spec）在 advisor 链外层；工具扇出并发/超时归 `HarnessToolCallingManager`
  （`buzhou.core.tool-timeout` 可配）。
