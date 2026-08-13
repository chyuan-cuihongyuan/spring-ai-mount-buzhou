# 17 — spill · 内容寻址 chunk hash 回读校验

**What to build:** 回读响应附切片 hash，「回读即原文」有密码学证明：腐化/TOCTOU 可检测，校验失败走既有读写失败非对称。

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] spill 落盘时记录 whole-content hash + 每切片 sha256（可选 Merkle root）
- [ ] 回读响应 envelope 附 `{data, byteRange, chunkSha256, handleRoot}`
- [ ] 校验失败走既有读侧 lenient（warning 透传）/ 写侧 strict（阻断）非对称
- [ ] Merkle root 进 evidence-id 的格式演进兼容旧 handle（不破坏既有指针）
- [ ] 端到端：篡改落盘数据→读侧 warning、写侧阻断
- [ ] spec 02（Spill）同步

> spec 12 §spill-17；[T45](../tickets/T45-spill-chunk-hash-verify.md)。源：git 62,540★ 概念锚点（对象名即内容 hash；切片级类 Merkle 摘要表）。
