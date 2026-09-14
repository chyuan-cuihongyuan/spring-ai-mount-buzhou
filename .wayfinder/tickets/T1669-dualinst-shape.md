---
id: T1669
title: EpisodeLedger 双实例组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1645
created: 2026-09-15
---

## Question

J 会话第 105 轮：情景台账双实例组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R88 钉了双台账（fact/episodic）独立性——同 stateStore 上 **EpisodeLedger 双实例**（重启语义：R40 序号恢复）的计数一致性未验证：双实例 record/recall 后统一静态读面累计正确。纯测试轮。

形状裁决：新增 `EpisodeLedgerDualInstanceTest`（buzhou-memory）——实例 A record 2 条 + 实例 B（同 store）record 1 条 + recall：静态计数跨实例累计、双守恒保持。零生产改动。
