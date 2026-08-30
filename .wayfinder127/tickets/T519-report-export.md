---
Type: task
Status: closed
---
## Question

PII 命中报表导出：JSONL 平铺 + 窗口语义 + 转义纪律。

## Resolution

done（2026-08-30）：impl-294；`guard/pii/PiiHitStatsJsonl`（export 静态面）
+ 红队 3 例（同序/空表/窗口循环）。
