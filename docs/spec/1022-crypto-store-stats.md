# 1022 — 加密消息存储操作计数读面

> 来源：J 会话第 23 轮 = effort #1022（[T1495](../../.wayfinder/tickets/T1495-crypto-store-stats-shape.md) / [T1496](../../.wayfinder/tickets/T1496-crypto-store-stats-verify.md) / impl 775）。与 spec 1012 同族：信封加密 ops 可视性（加密覆盖与透传残留直读）。

## Problem Statement

EncryptingMessageStore（spec 333）append 加密 / load·findById 解密 / 双方向透传（旧明文迁移友好 + 已信封幂等跳过）全程零计数：加密实际覆盖了多少消息、透传了多少（迁移期明文残留 vs 幂等跳过）不可见——「静态加密覆盖率」无法量化。

## 目标

- `EncryptingMessageStore` 增量（core.crypto，实例级）：`encrypted`（append 新加密）/ `decrypted`（load·findById 成功解密）/ `passthrough`（toCarrier 已信封跳过 + fromCarrier 旧明文透传，双方向合计）三 AtomicLong。
- 嵌套 record `CryptoStoreStats(long encrypted, long decrypted, long passthrough)` + `stats()` 快照。
- 解密失败照抛不计数（DATA_CORRUPTION 完整性优先语义不变）。

## 兼容性

纯增量读面：加密/透传/异常语义逐位不变；无新配置项。

## Out of Scope

- 解密失败计数（失败即异常上抛，归因在调用方——与 R13 openRejected 的「正常拒绝」路径不同性质）。
- EncryptingSummaryStore 同款扩散（结构同型，另行立项）。
