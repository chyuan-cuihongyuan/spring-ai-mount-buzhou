---
id: T1653
title: offload×cipher 加密联动组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1651
created: 2026-09-15
---

## Question

J 会话第 97 轮：溢出与加密联动的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R79 SpillCipher（溢出内容加密）与 R77 SpillOffloadHook（溢出落盘）联动——**开启加密后 offload 必伴随 encrypt 调用**的组合一致性无验证。纯测试轮第十二弹。

形状裁决：新增 `OffloadCipherComboTest`（buzhou-spill）——加密开启的 SpillStore 上 offload：encryptCalls 随 offloaded 同步增长 + 双读面各自守恒。零生产改动。
