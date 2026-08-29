# Spec 92 — 隔离舱健康面（effort #53）

> wayfinder map：`.wayfinder53/MAP.md`（T349–T350）。#84 fog 毕业生。

## Problem Statement

AgentBulkhead（spec 84）有 limitOf/inFlight 观测原语（部分包私有），但
`/actuator/buzhou` 快照与 actuator health 看不到舱状态——热点 agent 被限流时
运维无从得知配置了哪些舱、各舱水位如何。

## Solution

`BulkheadHealth implements BuzhouHealth`（mechanism `bulkhead`）：
- 全局舱未配置任何上限（NOOP 态）→ UNKNOWN + `{disabled: true}`（严格 DOWN
  纪律：未启用 ≠ DOWN）；
- 配置后 → UP + `{agents: {<agent>: "inFlight=N/limit=M"}}`（只列已配置 agent，
  16 条截断防御式；详情每次调用现读——取时快照）。
`AgentBulkhead` 新公共面 `configuredAgents()`（agent → limit 只读视图）。autoconfig
EndpointConfiguration 挂 `@ConditionalOnMissingBean` bean（读 global()——与装配
install 语义一致）。

## User Stories

1. 作为运维，我在健康快照看到各舱水位，所以限流风暴可定位到具体 agent。
2. 作为宿主，我要未启用时 UNKNOWN 不误报 DOWN，所以健康聚合不污染。

## Implementation Decisions

- 全局旋钮读取（非注入 bulkhead bean——装配链 install 全局后两者等价，且 NOOP
  默认态也能正确报 UNKNOWN）。
- 详情字符串形态 `inFlight=N/limit=M`（人读 + 有界——agent 名不进 micrometer
  tag 纪律的健康面等价处理）。

## Testing Decisions

- NOOP 态 UNKNOWN + disabled 详情；配置态 UP + per-agent 详情 + 名额持有期
  inFlight 原语联动；BuzhouHealthEndpoint 聚合含 bulkhead 段。

## Out of Scope

- 等待队列深度（Semaphore 无队列查询面）；per-agent 拒绝计数表；跨实例舱状态。

## Further Notes

- 与 error-signatures / webhook-outbox / session-index 并列的健康段家族第四员。
