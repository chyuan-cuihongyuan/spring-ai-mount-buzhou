---
id: T1693
title: evict×readRange 逐出复活组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1681
created: 2026-09-15
---

## Question

J 会话第 117 轮：逐出与回读复活组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R53 EvictHandleTool（逐出）与 R62 ReadRangeTool（回读复活：markRead 刷新 TTL）生命周期闭环——逐出后回读的**复活语义**（R88 生命周期测试无读面组合）+ 双 stats 交互。纯测试轮第十八弹。

形状裁决：新增 `EvictReadBackComboTest`（buzhou-spill）——同 HandleLifecycleRegistry 上 evict（evictions）→ readRange 回读（复活）→ 再 evict（再逐出）：双 stats 各自计数一致 + 守恒。零生产改动。
