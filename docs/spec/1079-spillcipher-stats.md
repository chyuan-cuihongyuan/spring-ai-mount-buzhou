# 1079 — Spill 加解密读面

> 来源：J 会话第 79 轮 = effort #1079（[T1613](../../.wayfinder/tickets/T1613-spillcipher-stats-shape.md) / [T1614](../../.wayfinder/tickets/T1614-spillcipher-stats-verify.md) / impl 831）。借鉴：AWS KMS 操作审计（加密服务自身操作分布是安全配置验证基座）。spill 安全域首轴。

## Problem Statement

`SpillCipher`（AES-256-GCM 溢出内容加解密）零计数——**加解密操作量与失败分布不可见**：加密面是否真实生效（encrypt 调用非零=启用了加密的部署确实在加密）无对账基座；解密失败（密钥错配/密文损坏）频次无信号。

## 目标

- `SpillCipher` 增量（spill，静态面）：四 `AtomicLong` 双组。
  - 加密侧：`encryptCalls` / `encryptFailures`；
  - 解密侧：`decryptCalls` / `decryptFailures`。
- 嵌套 `record SpillCipherStats(...)` + `stats()` + `resetForTest()`。
- 口径诚实：失败计数在异常外溢前落桶（异常语义不变——fail-fast 既有）。

## 兼容性

纯增量读面：encrypt/decrypt/isEncrypted/fromBase64Key 返回与异常语义逐位不变；静态面理由同 R46–R78 先例；无新配置项。

## Out of Scope

- 按内容大小分桶。
- 密钥轮换计数（MAGIC 版本面另轴）。
