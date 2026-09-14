---
id: T2155
title: 保留清扫新鲜度追踪器（RetentionSweepFreshness）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 28 轮：清扫调度新鲜度面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：RetentionSweeper 报告只推 listener 不落水位——清扫停摆静默；listener seam 既有（addSweepListener）——零侵入挂载点现成。

形状裁决：RetentionSweepFreshness implements Consumer<RetentionSweepReport>（opt-in）——sweepCount/lastSweepAt/staleMillis（调用方时钟）/maxGapMillis 间隔水位/failureCount（复用 fullySucceeded）+Snapshot/resetForTest；从未清扫 -1 哨兵。

Out of scope：告警联动；步骤分布；集群去重。
