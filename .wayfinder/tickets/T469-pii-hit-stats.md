---
Type: task
Status: closed
---
## Question

PII 命中统计：内置+自定义统一排行 + 钩子接线（自定义规则补盲）。

## Resolution

done（2026-08-30）：impl-284；`guard/pii/PiiHitStats` + PiiRedactionHook 双点
接线 + 红队 4 例 + guard 全量回归。
