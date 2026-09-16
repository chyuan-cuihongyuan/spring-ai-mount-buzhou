---
id: T3145
title: 复制计数器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多实例聚合计数怎么免双计收敛？（spec 2022 / effort #2022 / R23）

## Resolution

**CRDT G/PN-Counter 线程安全计数器 `ReplicatedCounter`
（core/concurrent）**：per-writer 分量（正计数负扣减 PN 合一，单调
自律）+value=Σ分量+components 字典序稳定快照（merge 载体）+merge
逐分量 max（at-least-once 重传不双计）——幂等/交换/结合三性质齐备。
与 LWW 寄存器成对（值域定序 vs 计数收敛）。
