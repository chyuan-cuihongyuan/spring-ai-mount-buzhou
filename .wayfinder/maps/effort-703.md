# effort #703 — 健康加权路由抑制原语

- 会话：G 会话 700 系第 4 轮 ｜ spec [703](../../../docs/spec/703-routing-health-dampener.md) ｜ 票 [T1006](../tickets/T1006-health-dampener.md)/[T1007](../tickets/T1007-health-dampener-verify.md) ｜ impl603
- 借鉴：HAProxy（≈5K GitHub 但引用量极高；同思想 Envoy outlier detection×路由联动）agent-check 权重反馈——健康检查结果动态调 server 权重

## 勘察（排重）

- WeightedChatModel（339/340）有 setWeight 运行时调权缝；ModelCircuitBreaker 状态机成熟但**无变迁监听缝**——路由与熔断两个韧性面互不知情：breaker OPEN 后路由仍按声明权重送流量（撞闸 429/拒）。
- 601 panic threshold 是驱逐面；fallback chain（FallbackChain）是异常后改道——都不是「事前按健康压权」。
- grep Dampener：零命中。

## 决定

①ModelCircuitBreaker 加 `addTransitionListener` 监听缝（异常隔离——listener 炸不伤状态机）；②`RoutingHealthDampener.attach(router, breaker, floorWeight)` 原语：→OPEN 压权至 floor（默认 1——保留最后通道，panic-threshold 思想不全零），→CLOSED 恢复声明权重（dampened 集追踪防重复），未匹配 beanName 静默忽略。零装配原语（零行为 by construction；宿主手工装配路由的场景直接用）。

## 测试

OPEN 压权+邻路不变/CLOSED 恢复/未匹配忽略+floor 校验+listener 异常隔离。

## 诚实边界

无渐变 ramp（恢复一步到位——渐变留给后续轮）；modelName↔beanName 按名相等匹配（映射表归宿主）；不自动装配（原语轮）。
