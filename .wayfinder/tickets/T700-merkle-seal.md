---
Type: task
Status: closed
---
## Question

`AuditChain.sealMerkle()` 时点封印（recordCount+rootHex+sealedAt，有界
32 印）+ seal→proof→verify 回路 + 封印后追加不影响旧印。

## Resolution

done（2026-09-08）：impl-377；回路/追加不变用例绿，buzhou-guard 全模块绿。
