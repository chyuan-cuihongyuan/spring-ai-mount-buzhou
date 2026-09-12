---
Type: task
Status: closed
---
## Question

`PiiVault`：vaultize 稳定令牌（sha256|salt 前 16 hex 去重）+ restore
还原/未知保留 + TTL 惰性过期（Clock 注入）+ maxEntries 有界 + 统计。

## Resolution

done（2026-09-12）：impl-410；往返/去重/过期/有界用例绿。
