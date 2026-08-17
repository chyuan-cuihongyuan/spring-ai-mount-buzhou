# 57 — 备模型降级链（T82 决策落地）

**What to build:** `buzhou-resilience` 新增 `fallback` 包（NamedFallbackModel + 降级执行逻辑并入 ResilienceAdvisor）；ResilienceProperties 增 `Fallback` 组（models / trigger-categories）；AutoConfiguration 按名解析 ChatModel bean（未命中 fail-fast）；ResilienceModule 编程式 overload；事件/指标/统计；M1 流式不降级文档化。

**Blocked by:** 56（熔断器，已 done）。

**Status:** done

- [ ] `NamedFallbackModel` record（name + ChatModel）+ Fallback 配置组（models 列表、triggerCategories 默认 [NETWORK,SERVER,TIMEOUT,AUTH]）+ fail-fast
- [ ] ResilienceAdvisor：主模型终态失败触发分类判定 → 逐备模型（熔断 beforeCall + 单次带 deadline 调用 + 终态记账）；成功返回 + switched 事件；全败上抛主因 + exhausted 事件；主模型 CIRCUIT_OPEN 拒绝恒触发降级
- [ ] deadline 执行器复用（提取 supplier 版本）
- [ ] AutoConfiguration `Map<String,ChatModel>` 按名解析、未命中名启动失败；ResilienceModule.configure 带 fallback overload
- [ ] ResilienceStats：fallbackSwitches/fallbackExhausted；指标两 counter；切换 WARNING 日志
- [ ] 测试：e2e（主败备成返回备回复+事件、熔断 OPEN 直达备模型且主模型零调用、全败上抛主因、AUTH 触发、CONTENT 不触发）、auto-config 未命中名 fail-fast、编程式路径

## Done

验证：`./mvnw -pl buzhou-resilience clean test` 73/73 绿（新增 FallbackChainEndToEndTest 6 用例：主败备成/CB-OPEN 直达备模型/全败上抛主因/CONTENT 不触发/无备模型保持 OPEN 快败/stats 计数）；starter clean test 5/5 绿。
落地：`fallback` 包（NamedFallbackModel + FallbackChain 触发语义）；ResilienceProperties 增 Fallback 组（models/trigger-categories 默认 [NETWORK,SERVER,TIMEOUT,AUTH]）+ 10 参兼容构造；ResilienceAdvisor 降级执行（CIRCUIT_OPEN 恒触发、备模型熔断前置闸跳级、单次带 deadline 直调、独立记账、全败上抛主因）；callWithDeadline 泛化 supplier；AutoConfiguration 按名解析 ChatModel bean 未命中 fail-fast；ResilienceModule 编程式 overload；stats 两计数 + 指标两 counter + WARNING 日志。
注意：本机 incremental compile 会产出损坏类（需 clean test，与 Mimosa 插桩相关）。
