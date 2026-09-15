---
id: T1705
title: 加密溢出×逐出组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1703
created: 2026-09-16
---

## Question

J 会话第 123 轮：加密溢出与逐出组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R97 加密溢出与 R53 逐出组合——加密 store 上溢出→逐出链的双读面（SpillOffloadStats/EvictHandleStats）独立性再验证。纯测试轮第十九弹。

形状裁决：新增 `CipherEvictComboTest`（buzhou-spill）——加密 store 溢出后 evict 句柄：双 stats 各自计数一致 + 守恒保持。零生产改动。
