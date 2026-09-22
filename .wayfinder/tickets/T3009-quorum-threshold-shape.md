---
id: T3009
title: 票数下限判定的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

崩溃/拜占庭两档容错的票数下限怎么算？（spec 1904 / effort #1904 / R105）

## Resolution`

**Paxos/BFT 票数下限纯计算 `QuorumThreshold`（core/transaction）**：
majority（⌊N/2⌋+1 简单多数）+ byzantineTolerance（⌊(N−1)/3⌋ 坏票
容忍）+ byzantineSize（3f+1 最小投票者）。voters≥1/faults≥0
fail-fast。落轮 grep 复核无占坑。
