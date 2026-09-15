# 1213 — 加密溢出×逐出组合测试轮

> 来源：J 会话第 123 轮 = effort #1223（[T1705](../../.wayfinder/tickets/T1705-cipherevict-shape.md) / [T1706](../../.wayfinder/tickets/T1706-cipherevict-verify.md) / impl 877）。纯测试轮第十九弹。R97 加密写入侧 × R53 逐出的生命周期闭环。

## Problem Statement

加密 store 上溢出→逐出链的双读面独立性——加密路径不改变逐出/溢出计数语义的组合验证缺失。

## 目标

新增 `CipherEvictComboTest`（buzhou-spill）：加密 DiskSpillStore 上溢出后 evict 句柄——SpillOffloadStats 与 EvictHandleStats 各自计数一致 + 守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 密钥管理面（另轴）。
