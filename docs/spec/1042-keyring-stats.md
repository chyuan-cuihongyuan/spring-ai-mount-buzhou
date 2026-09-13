# 1042 — 签名验钥分布读面

> 来源：J 会话第 42 轮 = effort #1042（[T1537](../../.wayfinder/tickets/T1537-keyring-stats-shape.md) / [T1538](../../.wayfinder/tickets/T1538-keyring-stats-verify.md) / impl 794）。借鉴：cert-manager / keyring ops（验钥失败水位=轮换配置与重放探测的第一信号）。与 R8/R13 同谱系（密钥生命周期可观测）。

## Problem Statement

SigningKeyRing.verifyKey 对**版本未知或低于 minVerifyVersion** 的验证请求静默返 null：退役钥验证尝试（旧钥重放探测）与轮换配置错位（minVerifyVersion 设错导致合法历史记录验不过）不可见——轮换后审计验证失败率无水位。

## 目标

- `SigningKeyRing` 增量（buzhou-guard audit 包，实例级）：`verifyAttempts` / `verifyKeyMisses` / `rotations` 三 AtomicLong。
  - verifyAttempts：每次 verifyKey 调用计（含 miss）；
  - verifyKeyMisses：版本未知**或**低于 minVerifyVersion 的拒绝次数（两类同桶——均为「无法验证」信号）；
  - rotations：rotate 成功次数。
- 嵌套 record `KeyRingStats(long verifyAttempts, long verifyKeyMisses, int rotations, int activeVersion, int minVerifyVersion)` + `stats()` 快照（含运行态上下文——排障一屏可读）。
- verifyKey/rotate 行为逐位不变（仅加计数）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按版本分桶命中分布（版本数随轮换增长——总量+miss 已足）。
- 拒绝升级异常/事件（验证方既有错误通道可见）。
