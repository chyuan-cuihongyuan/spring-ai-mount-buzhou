# Wayfinder Map — Buzhou 健康事件时间线（effort #405，D 会话第 6 轮）

> D 会话第 6 轮。勘察（2026-09-08）：grep `timeline` 全库零命中；312 告警
> 引擎只记「规则触发/恢复」，345 面板只显示**当前**状态——机制健康状态的
> **变迁史**（何时 UP→DOWN、DOWN→UP、抖了几次）无处可看。事故复盘
> 「当时到底谁先坏的」只能翻日志。
> 勘察换题记录：原拟「Retry-After 遵从」勘察发现 Classification.retryAfter
> +钳制到 maxBackoff 已存在（02 号票时代）——弃。

## Destination

`core.health` 变迁时间线（PagerDuty incidents 借鉴——状态事件流）：
`HealthTimeline`（有界环 256 默认，快照 diff 记变迁、首次现身记 from=null）
+ `HealthTimelineRecorder`（SmartLifecycle 周期轮询 health beans——与 312
> 同源 supplier，interval 默认 15s）+ `HealthTimelineJsonl`（export-path
声明即逐变迁落盘——ShadowComparisonJsonl 同旁路语义）+
`BuzhouTimelineEndpoint`（`/actuator/buzhou-timeline`：近期变迁 +
per-mechanism 变迁计数）。yml `buzhou.health.timeline.{enabled,interval,
capacity,export-path}`，默认关零行为变化。

## Notes

- 号段：spec 405 / T701–T702 / impl-378。
- 借鉴源：PagerDuty incident timeline（状态事件流）+ Grafana annotations。
- 纪律：环有界（容量=内存上界）；JSONL 失败吞+计数（旁路不放大）；
  轮询与告警引擎独立调度（关时间线不影响告警）。

## Out of scope

- 变迁事件进 SessionEvent 流（观察通道分离——健康面不进会话事件）；
  持久化时间线（重启清零为诚实边界）；告警联动（312 已管阈值语义）。

## Tickets

- [x] [T701 HealthTimeline + 轮询记录器](../tickets/T701-health-timeline.md)
- [x] [T702 JSONL 导出 + 端点 + yml 装配](../tickets/T702-timeline-export-endpoint.md)
