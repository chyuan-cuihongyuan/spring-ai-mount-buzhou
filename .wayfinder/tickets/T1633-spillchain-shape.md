---
id: T1633
title: spill 域 offload+evict 生命周期组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1631
created: 2026-09-15
---

## Question

J 会话第 89 轮：spill 双 hook 组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R77 SpillOffloadHook（溢出落盘）与 R53 EvictHandleTool（句柄逐出）构成完整生命周期（溢出→逐出→墓碑→回读复活）——**两读面在生命周期中的计数一致性**无验证。纯测试轮第七弹。

形状裁决：新增 `SpillLifecycleReadoutTest`（buzhou-spill）——溢出（offloaded）→ 逐出（evictions）→ 全程 SpillOffloadStats 与 EvictHandleStats 各自守恒保持、互不串账。零生产改动。
