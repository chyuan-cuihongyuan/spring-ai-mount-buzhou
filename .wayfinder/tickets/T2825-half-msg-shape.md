---
id: T2825
title: 半消息审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

「做事」与「发事件」跨系统的原子性怎么留痕审计？（spec 1812 / effort #1812 / R13）

## Resolution

**RocketMQ 事务半消息+回查思想纯读面 `HalfMessageAudit`
（core/transaction）**：Intent(key, state, age) 三态事实（HALF/COMMITTED/
ROLLED_BACK，构造器核契约）；audit(staleThreshold, intents) → Census
（halves 滞留/committed/rolledBack/staleHalves 超阈回查候选，阈含边界）+
resolutionRatio/pendingRatio（-1 哨兵）。与 CompensatingBatch 互补：事前
意图留痕 vs 事后补偿。

