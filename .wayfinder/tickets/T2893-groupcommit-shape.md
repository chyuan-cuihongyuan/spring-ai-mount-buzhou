---
id: T2893
title: 组提交账面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

合并刷盘值不值怎么算账？（spec 1846 / effort #1846 / R47）

## Resolution

**MySQL group commit/PostgreSQL commit_delay 思想纯记账
`GroupCommitAccounting`（core/fs）**：Account(writes, flushes, soloCost,
batchedCost) 契约（0≤flushes≤writes、有写必有刷——一刷多写是合并）+
amortizationRatio 摊薄倍数（-1 哨兵）+ savedFlushes 省刷数（×刷盘单价
=SSD 寿命节省）+ savingsNanos 净省（可为负——不划算面诚实）+
savingsRatio（零分母 -1）。

