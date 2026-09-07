# Wayfinder Map — Buzhou 健康告警规则（effort #312，C 会话第 13 轮）

> C 会话第 13 轮。BuzhouHealth 健康面（机制 DOWN 严格口径）只能被 actuator
> 轮询看到——机制失能没有进程内告警通道（运维要自己盯端点）。

## Destination

`AlertRuleEngine`（core.health）：yml 声明规则（机制 → DOWN 触发 + for
持续窗）周期评估健康面；触发/恢复经回调通知（宿主接分页/webhook）+
计数 + WARN 日志。Grafana ruler 思想：规则即声明、数据源即健康面、
通道即回调。

## Notes

- 号段：spec 312 / T615–T616 / impl-335。
- metrics 只写不可读（勘察结论）——健康面是唯一可读有界源。

## Decisions so far

- for 持续窗防抖（flap 不触发）；触发→恢复双向通知。
- 规则引用不存在的机制 = 启动 fail-fast（yml 错该红）。
- UNKNOWN ≠ DOWN（未启用机制不告警——BuzhouHealth 契约）。

## Out of scope

- 指标阈值规则（metrics 不可读——引入可读注册表归观测族后续轮）；
- 告警路由/静默（通道归宿主回调）。

## Tickets

- [x] [T615 AlertRuleEngine（评估/防抖/双向通知/计数）](../tickets/T615-alert-rules.md)（impl-335）
- [x] [T616 yml 装配 + 回归](../tickets/T616-alert-close.md)（impl-335）
