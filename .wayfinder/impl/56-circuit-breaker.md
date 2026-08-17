# 56 — 模型熔断器（T81 决策落地）

**What to build:** `buzhou-resilience` 新增手写熔断器：`circuit` 包（ModelCircuitBreaker 进程级注册表 + CircuitState + ModelCircuitOpenException）；ResilienceAdvisor 接线（重试环前 fail-fast 检查、每逻辑调用记一次三态结果、流式入口检查与终态记录）；ResilienceProperties 增 `circuit` 嵌套组（9 参构造兼容委托）；ResilienceStats 增计数与 states 详情；配置 fail-fast；事件/指标/日志齐备。

**Blocked by:** None.

**Status:** done

- [ ] `CircuitState` + `ModelCircuitOpenException` + `ModelCircuitBreaker`（三态结果、计数窗口失败率跳闸、冷却、半开单探测、按 modelName 分桶、线程安全）
- [ ] ResilienceProperties 增 `Circuit` 嵌套 record + 9 参兼容构造 + compact 校验；ResilienceModule.validate 增 fail-fast
- [ ] ResilienceAdvisor 新全参构造（circuit+modelName），adviseCall/adviseStream 接线；ResilienceModule customizer 创建进程级实例注入
- [ ] ResilienceStats：circuitRejections/circuitTrips 计数 + details states（有界）；指标三件（counter×2 + gauge）；状态迁移日志
- [ ] 测试：状态机单测（跳闸/拒绝/半开探测成功回闭/失败回开/IGNORED 不进窗口）、e2e（连续 NETWORK 失败跳闸后模型零调用、恢复探测成功）、流式 open 拒绝、配置 fail-fast、stats/事件断言

## Done

验证：`./mvnw -pl buzhou-resilience test` 67/67 绿（新增 circuit 单测 12 + e2e 5）；`-pl buzhou-spring-boot-starter clean test` 5/5 绿；`-pl examples clean test` 62/62 绿（无回归）。
落地：`circuit` 包三件（ModelCircuitBreaker 进程级注册表 + CircuitState + ModelCircuitOpenException）；三态结果计数（FAILURE=NETWORK/SERVER/TIMEOUT 字符串口径，RATE_LIMIT/CONTENT/AUTH/UNKNOWN IGNORED 不进窗口）；计数窗口 ring buffer 失败率跳闸；OPEN 冷却 + HALF_OPEN 单探测（占位拒绝 + 2×cooldown 超时逃生）；迟到样本丢弃；ResilienceAdvisor 逻辑调用级接线（call 前置闸 + finally 记账；流式入口 Flux.error + doOnComplete/doOnError）；ResilienceProperties 增 Circuit 组 + @ConstructorBinding（9 参兼容构造保留）+ fail-fast；ResilienceModule 进程级创建；ResilienceStats 计数/states/UP 语义；指标 counter×2 + gauge；事件×2 走会话通道。
