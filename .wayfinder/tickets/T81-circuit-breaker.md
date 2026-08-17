---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

模型熔断器怎么做？现状：ResilienceAdvisor 只有指数退避重试（RATE_LIMIT/NETWORK），provider 持续故障=重试耗尽直抛，无半开探测（spec 15 明确「熔断/重试预算：开放问题」）。决策点：手写 CB（不引 resilience4j，沿用 effort#4 手写方针）的状态机（closed/open/half-open）、滑动窗口失败率判定口径（按 ErrorCategory 哪些计入）、半开探测恢复语义、与 ResilienceStats/BuzhouHealth/指标族/日志的联动、配置面与 fail-fast 校验。产出 spec 15 增量 + impl 56。

## Resolution

AFK 自决（用户 2026-08-15 授权，可推翻）：

1. **形态**：手写 CB，不引 resilience4j（≥10K★ 但运行时依赖最小化方针优先，与 retry 同理由）。新包 `circuit`：`ModelCircuitBreaker`（进程级注册表，按 modelName 分桶，与 ModelRateLimiter 同口径）+ `CircuitState`(CLOSED/OPEN/HALF_OPEN) + `ModelCircuitOpenException`。
2. **进程级共享**：在 `ResilienceModule.configure()` 创建一次、经 customizer 闭包注入所有会话（修 RateLimiter 每会话分桶的语义问题留给 T84）。Spring 路径 configure 每 context 一次=进程级。
3. **计数口径**（三态结果，非二值）：SUCCESS / FAILURE / IGNORED。FAILURE=终态失败且 category ∈ `failure-categories`（默认 [NETWORK, SERVER, TIMEOUT]）；RATE_LIMIT（背压，归限流器）、CONTENT（内容治理）、AUTH（配置错误，不遮蔽）、UNKNOWN（不明原因不盲跳）IGNORED 不进窗口。
4. **状态机**：计数窗口（默认 size=20）失败率 ≥ threshold（默认 0.5）且样本 ≥ minCalls（默认 5）→ OPEN；OPEN 冷却 `open-cooldown`（默认 30s）内调用直接 `ModelCircuitOpenException`（不进重试分类，直上 onModelError，与限流拒绝同语义）；冷却后单探测（probeInFlight 原子占位，并发拒绝）进 HALF_OPEN：探测 FAILURE→回 OPEN（重计冷却），SUCCESS/IGNORED→CLOSED（窗口重置）。
5. **接线**：ResilienceAdvisor 持 circuit+modelName（新增全参构造，旧构造委托 null=禁用）；adviseCall 在重试环之前 fail-fast 检查、每次**逻辑调用**（非 attempt）记一次结果；adviseStream 入口检查 + doOnComplete 记成功 + 错误路径记失败。
6. **运维面**：ResilienceStats 增 circuit-rejected/trips 计数 + details 暴露有界 states map；机制健康恒 UP（断路器工作正常，宕的是 provider——不误 DOWN，严格 DOWN 语义），OPEN 态经 details+指标可见；指标 `buzhou.resilience.circuit-rejected`/`circuit-tripped` 计数器 + `circuit-open` gauge（core 单口径家族）；状态迁移 INFO/OPEN WARNING 日志；事件 `circuit.state-changed`/`circuit.call-rejected` 走当次调用会话通道（进程级组件+会话级可见性）。
7. **配置**：`buzhou.resilience.circuit.*`（enabled 默认 true、window-size/min-calls/failure-rate-threshold/open-cooldown/failure-categories），fail-fast：window≥2、1≤minCalls≤window、0<threshold≤1、cooldown>0。
8. **默认启用**（保守阈值下安全：真实 provider 故障才跳闸），与模块「引入即生效」哲学一致。
