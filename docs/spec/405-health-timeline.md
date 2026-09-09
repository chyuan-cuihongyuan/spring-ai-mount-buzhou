# Spec 405 — 健康事件时间线（effort #405）

> wayfinder map：`.wayfinder/maps/effort-405.md`（T701–T702）。D 会话第 6 轮。

## Problem Statement

机制健康状态的变迁史无处可看：312 只记规则触发/恢复、345 只显示当前
状态——「何时 UP→DOWN、抖了几次、谁先坏的」事故复盘只能翻日志；
抖动（flapping）机制的识别没有数据面。

## Solution

`core.health` 变迁时间线（PagerDuty incident timeline 借鉴）：

- **`HealthTimeline`**：有界环（默认 256，容量=内存上界）。`record(
  Map<String,Status> snapshot, at)` 与上一快照 diff，仅变迁入环；
  首次现身记 `from=null`（初见语义）；`entries()`（旧→新）、
  `transitionCounts()`（mechanism→变迁次数——抖动识别面）。
- **`HealthTimelineRecorder`**（SmartLifecycle）：周期轮询 health beans
  （与 312 同源 supplier 口径；interval 默认 15s）→ diff → 入环 +
  可选 sink（导出用）。独立调度——关时间线不影响告警引擎。
- **`HealthTimelineJsonl`**：`export-path` 声明即逐变迁追加 JSONL
  （{at, mechanism, from, to}；每行 flush；IO 失败吞+计数——旁路语义，
  ShadowComparisonJsonl 同款）。
- **`BuzhouTimelineEndpoint`**（`/actuator/buzhou-timeline`，只读）：
  近期变迁 + per-mechanism 计数 + 容量。bean 缺席 = 端点不在（未开零变化）。
- yml：`buzhou.health.timeline.{enabled=false, interval=15s, capacity=256,
  export-path}`。

## User Stories

1. 作为 SRE，我想看每个机制的状态变迁时间线，所以 事故复盘「谁先坏
   的」有数据面不用翻日志。
2. 作为运维，我想看 per-mechanism 变迁计数，所以 抖动机制（高频
   UP↔DOWN）一眼可辨。
3. 作为宿主，我想变迁落盘 JSONL，所以 时间线可离线归档分析。

## Implementation Decisions

- diff-only 入环（无变迁零记录——环不被无信息行占满）。
- 观察通道分离：时间线不进 SessionEvent 流。

## Testing Decisions

- diff 语义（无变迁零行/变迁一行/首次现身 from=null）；
- 容量封顶（旧项挤出）；
- recorder 轮询驱动（stub health 翻转状态）；
- JSONL 落盘行 + 端点载荷 + yml 装配（默认关无 bean）。

## Out of Scope

- 变迁进 SessionEvent；持久化时间线；告警联动。

## Further Notes

- 新公共类型 `HealthTimeline`（嵌套 `Entry`）/ `HealthTimelineRecorder` /
  `HealthTimelineJsonl` / `BuzhouTimelineEndpoint` /
  `BuzhouHealthTimelineProperties` 随轮 regenerate 快照。
