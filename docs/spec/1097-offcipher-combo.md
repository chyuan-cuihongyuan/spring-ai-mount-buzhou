# 1097 — offload×cipher 加密联动组合测试轮

> 来源：J 会话第 97 轮 = effort #1097（[T1653](../../.wayfinder/tickets/T1653-offcipher-shape.md) / [T1654](../../.wayfinder/tickets/T1654-offcipher-verify.md) / impl 849）。纯测试轮第十二弹。

## Problem Statement

R79 SpillCipher（加密）与 R77 SpillOffloadHook（溢出落盘）联动——**开启加密后 offload 必伴随 encrypt 调用**的组合一致性无验证：加密配置失效（encrypt 零调用）无信号。

## 目标

新增 `OffloadCipherComboTest`（buzhou-spill）：加密开启的 SpillStore 上 offload——encryptCalls 随 offloaded 同步增长 + SpillOffloadStats 守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 密钥管理面（另轴）。
