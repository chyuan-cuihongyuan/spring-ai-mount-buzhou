# 1106 — cipher×readBack 组合测试轮

> 来源：J 会话第 106 轮 = effort #1106（[T1671](../../.wayfinder/tickets/T1671-cipherread-shape.md) / [T1672](../../.wayfinder/tickets/T1672-cipherread-verify.md) / impl 858）。纯测试轮第十二弹（R97 姊妹：那次钉加密写入侧，本轮钉解密回读侧）。

## Problem Statement

R79 SpillCipher（加密）与 R62 ReadRangeTool（回读）组合——加密存储上回读的**解密调用联动**（decryptCalls 随 reads 增长）无验证：解密面失效（密钥错配致回读失败）组合信号缺失。

## 目标

新增 `CipherReadBackComboTest`（buzhou-spill）：加密 DiskSpillStore 上写入密文→回读——decryptCalls 随 reads 增长 + ReadRangeStats 守恒保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 密钥轮换组合（另轴）。
