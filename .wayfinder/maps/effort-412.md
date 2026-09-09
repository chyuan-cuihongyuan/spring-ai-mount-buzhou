# Wayfinder Map — Buzhou 时间桶预聚合（effort #412，D 会话第 13 轮）

> D 会话第 13 轮。勘察（2026-09-08）：dashboard 统计全是**点态**——
> per-session stats/per-tool/per-model 聚合无时间轴；「每小时 token 消耗/
> 错误趋势」这种最常用的运营图表无数据面。M3/Prometheus downsampling
> 的固定桶预聚合缺失。
> 勘察换题记录：原拟「事件偏移重放」——WebhookEventForwarder.
> replayDeadLetters 已有全量重放（spec 37 收口），偏移细化增益薄弃。

## Destination

`DashboardQueryService.rollups(from, to, bucket)`（M3/Prometheus
downsampling 借鉴——固定桶+空桶补齐）：全会话枚举（listSessionSummaries
翻页）→ spansOfSession 过滤 TURN/MODEL_CALL/TOOL_CALL 三类入窗 →
epoch 对齐 floor 分桶 → `TimeBucket{start, turns, modelCalls, toolCalls,
errors, promptTokens, completionTokens}` 升序、**空桶补齐**（图表连续性）
；桶数 >1000 fail-fast（payload 上界）。HTTP：`GET /api/rollups?from&to&
bucket`（ISO-8601 时间 + 5m/1h 形态时长）。

## Notes

- 号段：spec 412 / T715–T716 / impl-385。
- 借鉴源：M3（36k★）fixed window downsampling；Prometheus rate 需
  连续桶（空桶补齐的理据）。
- 纪律：跨会话 O(n) 扫描为 dashboard 规模（翻页枚举不一次全载）；
  只读查询零行为变化；桶对齐 epoch（跨实例对齐口径一致）。

## Out of scope

- 预聚合持久化/后台定时物化（查询时聚合——真需求再物化）；多维分组
  （per-model 桶——扩散候选）；P99 时延桶（duration 直方图另议）。

## Tickets

- [x] [T715 rollups 聚合语义](../tickets/T715-rollups-aggregation.md)
- [x] [T716 HTTP 端点 + 参数解析](../tickets/T716-rollups-endpoint.md)
