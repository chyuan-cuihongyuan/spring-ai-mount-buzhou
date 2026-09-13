# 726 — 事件类型分布读数

> 来源：G 会话第 27 轮 = effort #726（543/720 观测分布族的事件对偶）/ [T1052](../../.wayfinder/tickets/T1052-event-type-dist.md) / [T1053](../../.wayfinder/tickets/T1053-event-type-dist-verify.md) / impl 626。

## Problem

EventType 枚举+EventRecord 落库齐备，但「这段时间哪类事件在刷屏、哪些事件从不发生」无聚合面——异常事件风暴（如 circuit.call-rejected 连发）与静默缺失（某生命周期事件从未出现）都要全量拉记录自算。

## Solution

Grafana Loki top-k 聚合思想（≈23K star）：

- `EventTypeDistribution`（core/observability，纯函数）：`of(List<EventRecord>)` → `Report(rows, total, distinctTypes)`——rows=type→count **降序**（同计数 type 名字典序稳定）；`topType()` = 占比最高的类型及其份额（空表 null）。
- 类型字符串不假设闭集（EventType 枚举外的自定义类型照常聚合）。

## Out of Scope

- 时间窗/会话过滤（调用方投影）；告警归 312。
