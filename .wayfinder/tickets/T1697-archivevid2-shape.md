---
id: T1697
title: 归档×readRange 生命周期组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1695
created: 2026-09-15
---

## Question

J 会话第 119 轮：归档与分段回读联动的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R75 SessionArchiver（归档级联删除）与 R62 ReadRangeTool（分段回读）联动——归档级联删除后回读同会话 evidence 的**组合行为**（R109 已做 MemoryStore 直查，本轴走 ReadRangeTool 全链路含读面）。纯测试轮第十九弹。

形状裁决：新增 `ArchiveReadRangeComboTest`（buzhou-memory）——溢出→归档→回读链：归档删除活数据后 readRange 回查按 store 语义（miss 稳定）+ ReadRangeStats 守恒。零生产改动。
