# 1012 — 加密封存操作生命周期计数读面

> 来源：J 会话第 13 轮 = effort #1012（[T1475](../../.wayfinder/tickets/T1475-seal-stats-shape.md) / [T1476](../../.wayfinder/tickets/T1476-seal-stats-verify.md) / impl 765）。借鉴：age / OpenSSL 运维实践——密文操作计数与失败率是密钥轮换错配的第一信号。

## Problem Statement

EncryptedSessionExport（spec 510）seal/open 全程零计数：封了多少、开了多少、**开失败多少**不可见。open 有三条 DATA_CORRUPTION 拒绝路径（非封缄串 / 密钥失配或密文篡改 / 解密成功但载荷非导出 JSON）——失败率陡增即密钥轮换错配或篡改探测的第一信号，现无读面可察。

## 目标

- `EncryptedSessionExport` 增量（core.session，实例级）：`sealed` / `opened` / `openRejected` 三 AtomicLong。
- 嵌套 record `SealStats(long sealed, long opened, long openRejected)` + `stats()` 快照。
- 计数点：seal 成功 sealed+1；open 成功 opened+1；open 三条拒绝路径各 openRejected+1（异常仍照抛——计数不吞错）。`isSealed` 静态判定不涉操作不计数。

## 兼容性

纯增量读面：seal/open 返回值与异常语义逐位不变；无新配置项。

## Out of Scope

- 按密钥环版本分桶（keys 无稳定标识，基数纪律）。
- 进程级聚合（实例即装配粒度，与 hook bean 同理）。
