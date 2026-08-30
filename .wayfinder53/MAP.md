# Wayfinder Map — Buzhou 隔离舱健康面（effort #53，50 轮自迭代第 18 轮）

> effort #53，延续 #52（T347–T348 / impl-238）。主线：**#84 fog 毕业生**——
> AgentBulkhead 有 configuredAgents()/inFlight() 观测原语（部分包私有），但
> /actuator/buzhou 快照看不到舱状态。

## Destination

`BulkheadHealth implements BuzhouHealth`（mechanism=bulkhead）：未配置任何上限
（全 NOOP）→ UNKNOWN + disabled 详情（严格 DOWN 纪律：未启用≠DOWN）；配置后
UP + per-agent `inFlight=N/limit=M` 有界详情（16 agent 截断防御式）；autoconfig
EndpointConfiguration 挂 bean（读 AgentBulkhead.global()）；AgentBulkhead 新公共面
`configuredAgents()`（只读视图）。

## Notes

- 借鉴：与 ErrorSignaturesHealth（spec 85）同族——第三个 BuzhouHealth 实现。

## Decisions so far

- 全局旋钮读取（AgentBulkhead.global()）——与装配语义一致（bulkhead bean 装配时
  install 过全局）。
- 详情取时快照（不缓存——健康面每次调用现读）。

## Not yet specified

- 等待队列深度（信号量无队列查询面——需扩展 Semaphore 包装）；per-agent 拒绝计数
  （buzhou.bulkhead.rejected 无 tag——有界 agent 名不可进 tag，需进程内表）。

## Out of scope

- 沿用 #7–#52；跨实例舱状态。

## Tickets

- [x] [T349 BulkheadHealth + configuredAgents() 公共面 + 装配](tickets/T349-bulkhead-health.md)（impl-239）
- [x] [T350 3 例红队（UNKNOWN/UP 详情/端点聚合）+ 收口](tickets/T350-bulkhead-health-close.md)
