# 839 — 泄漏疑似对象聚合器

> 来源：H 会话第 40 轮 = effort #839 / [T1179](../../.wayfinder/tickets/T1179-leak-suspect-aggregator.md) / [T1180](../../.wayfinder/tickets/T1180-leak-suspect-aggregator-verify.md) / impl 592。
> 换题注记：原 R40 滚动导出统计半撞（RollingJsonlWriter 已有轮转计数）——启用 S9。

## Problem

资源泄漏检测逐条报告：同一处反复漏 vs 多处散漏不可辨——「泄漏排行」缺聚合面（修一处治一片还是处处着火）。

## Solution

`LeakSuspectAggregator`（core.leak，实现 LeakListener）：

- **聚合**：onLeak 按描述键（截 64 稳键）聚合计数/最大龄/最近时刻；键封顶 32+溢出桶。
- **排行**：snapshot count 降序典序破平+totalLeaks+truncated。
- **挂接**：实现 LeakListener 直接可挂；检测器零变更。

## 兼容性

纯新增；ResourceLeakDetector 零变更。

## 诚实边界

键粒度归检测器描述；快照近似口径（per-agg 锁非全局）；聚合不阻止泄漏。
