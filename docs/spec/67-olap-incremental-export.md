# Spec 67 — OLAP 增量导出（effort #27）

> wayfinder map：`.wayfinder27/MAP.md`（T285–T286）。OSS 借鉴：Langfuse ingestion
> cursor / Helicone 水位分页。

## Problem Statement

全量 JSONL 导出（spec 60）每次 dump 所有会话——数据量随时间线性膨胀，周级分析场景
重复搬运不变数据。

## Solution

`exportAllSince(Writer, Instant since)`：只导出 lastActivityAt ≥ since 的会话；返回
值携带本次最大 activityAt 作为下次水位（空结果水位不变）。会话粒度 at-least-once：
会话有新数据则该会话全量重导（OLAP 端按 spanId/eventId 主键 upsert）。

## User Stories

1. 作为数据工程师，我要增量导出，所以周级 pipeline 不重复搬运历史。
2. 作为数据工程师，我要返回新水位，所以断点续传无需自算。
3. 作为红队，我要空结果水位不变、边界会话（= since）被包含，所以水位语义无洞。
4. 作为既有用户，我要全量导出行为零变化，所以升级零风险。

## Implementation Decisions

- JsonlExportResult 加第 5 组件 waterline（Instant nullable；4 参兼容构造保留）。
- 过滤在导出侧（store 面不变——listSessionSummaries 已按活跃降序）。

## Testing Decisions

- 水位过滤/新水位/空结果/边界包含/全量导出回归。

## Out of Scope

- span 粒度增量；定时任务；新键。

## Further Notes

- 重复导出 = at-least-once（主键 upsert 契约与 spec 60 同）。
