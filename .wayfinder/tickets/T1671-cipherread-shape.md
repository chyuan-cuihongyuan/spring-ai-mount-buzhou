---
id: T1671
title: cipher×readBack 组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1655
created: 2026-09-15
---

## Question

J 会话第 106 轮：加密与回读组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R79 SpillCipher（加密）与 R62 ReadRangeTool（回读）组合——加密存储上回读的解密调用联动（decryptCalls 随 reads 增长）无验证。纯测试轮第十二弹。

形状裁决：新增 `CipherReadBackComboTest`（buzhou-spill）——加密 DiskSpillStore 上写入密文→回读：decryptCalls 随 reads 增长 + 双读面各自守恒。零生产改动。
