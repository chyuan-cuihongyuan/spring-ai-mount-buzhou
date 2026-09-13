# 742 — sweepOrphans 保留计数读数

> 来源：G 会话第 43 轮 = effort #742（impl-38 孤儿扫描的证据面）/ [T1086](../../.wayfinder/tickets/T1086-sweep-retained.md) / [T1087](../../.wayfinder/tickets/T1087-sweep-retained-verify.md) / impl 643。

## Problem

sweepOrphans 只返回 deleted 数——被 fork 引用而**物理保留**的孤儿（retained 分支）完全不可见：保留数持续增长 = fork 证据在无限堆积（引用者不关闭），治理无信号。

## Solution

- `totalRetainedOrphans()`：累计保留数（跨 sweep 累计）。
- `lastSweepRetained()`：最近一次 sweep 的保留数（-1 哨兵=从未执行）。
- 只读不清理语义变化（sweep 行为逐位不变——只是把局部变量升格为证据面）。

## Out of Scope
保留者的强制关闭（归 fork 生命周期机制）；按引用者细分。
