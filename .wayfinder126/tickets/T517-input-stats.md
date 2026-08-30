---
Type: task
Status: closed
---
## Question

输入侧 PII 命中进 PiiHitStats：双点打点 + 共用自定义提取器。

## Resolution

done（2026-08-30）：impl-293；PiiInputRedactionHook 双点接线 +
PiiHitStats.extractCustomRuleNames 共用 + 红队 1 例 + guard 全量 116 绿。
