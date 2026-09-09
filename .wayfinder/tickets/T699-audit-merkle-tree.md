---
Type: task
Status: closed
---
## Question

`AuditMerkleTree.of`（叶=JCS 同摘要基、奇数复制末叶、空树=sha256("")）
+ `proof(recordId)` 包含证明 + 静态 `verify`。

## Resolution

done（2026-09-08）：impl-377；每叶可验/篡改即假/奇偶树/空树用例绿。
