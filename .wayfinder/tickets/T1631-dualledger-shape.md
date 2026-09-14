---
id: T1631
title: memory 双台账组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1627
created: 2026-09-15
---

## Question

J 会话第 88 轮：memory 双台账组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R63 BiTemporalFactLedger（事实台账）与 R55 EpisodeLedger（情景台账）双台账同域共存——**双台账读面独立性与 reset 隔离**无验证。纯测试轮第六弹。

形状裁决：新增 `DualLedgerReadoutTest`（buzhou-memory）——fact 写入与 episodic record/recall 交叉后，FactLedgerStats 与 EpisodicMemoryStats 各自计数互不串账、reset 独立隔离。零生产改动。
