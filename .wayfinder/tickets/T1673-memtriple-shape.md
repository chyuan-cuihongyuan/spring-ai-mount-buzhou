---
id: T1673
title: memory 三读面大组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1671
created: 2026-09-15
---

## Question

J 会话第 107 轮：memory 三读面大组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R59 compact_now/R55 episodic/R63 fact ledger 三读面同会话全交叉——R87/R88 两两组合后三读面大组合的互不串账收口。纯测试轮第十三弹。

形状裁决：新增 `MemoryTripleReadoutTest`（buzhou-memory）——三读面交叉调用后各自守恒保持 + 互不串账 + reset 独立隔离。零生产改动。
