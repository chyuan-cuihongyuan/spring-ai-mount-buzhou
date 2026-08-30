---
Type: task
Status: closed
---
## Question

exportAllSince(Writer, Instant)：listSessionSummaries 过滤 lastActivityAt ≥ since；
返回 JsonlExportResult 扩展 waterline（本次最大 activityAt；空 = since 原样）。

## Resolution

done（2026-08-29）：impl-213；9 例（含游标偏移修复）。
