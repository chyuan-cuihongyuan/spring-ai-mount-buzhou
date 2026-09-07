# Wayfinder Map — Buzhou OLAP 增量导出（effort #27）

> effort #27，延续 #5–#26（累计 173 轮 / T1–T284 / impl 1–212）。
> 主线：**#20 fog 毕业生**——全量 JSONL 导出每次重 dump 全部会话；周级分析只需要增量。
> 借鉴 Langfuse/Helicone 摄取面的水位（cursor）语义：导出带 since 水位 + 返回新水位。

## Destination

`ObservabilityJsonlExporter.exportAllSince(Writer, Instant since)`：只导出
lastActivityAt ≥ since 的会话；返回 `JsonlExportResult` 携带本次导出观测到的最大
activityAt 作为下次水位（空结果水位不变）；全量导出零变化；零新键。

## Notes

- 外部事实源：Langfuse ingestion cursor / Helicone 分页水位。本地裁定：会话粒度
  水位（span 粒度需索引扫描下沉 store——fog 留位）；重复导出边界（同会话部分新数据）
  = 该会话全量重导（at-least-once 语义，OLAP 端按主键 upsert——诚实入档）。

## Decisions so far

- 水位 = SessionSummary.lastActivityAt（既有面零新查询）；返回 record 复用
  JsonlExportResult + 新字段 waterline（nullable——全量导出不带）。

## Not yet specified

- span 粒度增量；导出幂等键列（eventId/spanId 已是——OLAP upsert 依据）。

## Out of scope

- 沿用 #7–#26；定时任务；新配置键。

## Tickets

- [x] [T285 exportAllSince 水位导出 + waterline 返回](../tickets/T285-incremental.md)（impl-213；游标偏移按枚举位推进——过滤不改偏移）
- [x] [T286 增量红队（水位过滤/新水位/空结果/全量回归）+ 文档 + verify + 收口](../tickets/T286-incremental-close.md)
