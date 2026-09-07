# Wayfinder Map — Buzhou 工具熔断 yml 装配（effort #306，C 会话第 7 轮）

> C 会话第 7 轮。spec 131/165 的 `ToolCircuitBreaker(Hook)`（resilience4j）
> standalone——hook 靠宿主编程注册，无 yml 面（fog 227「新 hook 配置面族」首项）。

## Destination

`buzhou.tools.circuit.enabled=true` 即装配 `ToolCircuitBreakerHook` bean
（BuzhouHook 经 List 自动收集进 RuntimeConfig）；window-size / failure-rate-
threshold-percent / cooldown / half-open-trials 四参可调（默认 20/50/60s/3）；
默认关零变化。

## Notes

- 号段：spec 306 / T603–T604 / impl-329。
- 原计划「泳道公平模式」勘察发现 173 已用公平 Semaphore——本轮换 fog 真缺口。

## Decisions so far

- 参数校验与 ToolCircuitBreaker.Config 同口径（装配层 fail-fast）。

## Out of scope

- 跨实例共享熔断后端（Redis 族后续轮）；per-tool 熔断参数覆盖（后续按需）。

## Tickets

- [x] [T603 circuit 属性组 + hook 装配 bean](tickets/T603-circuit-yml.md)（impl-329）
- [x] [T604 装配回归（默认关/enabled/参数绑定/非法拒绝）](tickets/T604-circuit-close.md)（impl-329）
