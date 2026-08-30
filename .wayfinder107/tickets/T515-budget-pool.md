---
Type: task
Status: closed
---
## Question

保底配额 + 弹性借用 + 归还的预算池。

## Resolution

done（2026-08-30）：impl-291；ElasticBudgetPool（max(base,held) 护底口径 +
借走不召回 + borrowed/denied 计数 + snapshot）。
