# Spec 1846 — 组提交账面（effort #1846，R47）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2893–T2894，impl 1447）。借鉴：
> MySQL group commit / PostgreSQL commit_delay——多写合并一次 fsync 摊薄
> 刷盘成本，值不值由账面说话。

## Problem Statement`

滚动写路径（OLAP JSONL/审计日志）逐写刷盘太贵，但合并刷盘有迟滞代价
（等批窗口）——「合并了几倍、省了几刷、净省多少」没有账面，组提交开不开
 只能拍脑袋。

## Solution

`GroupCommitAccounting`（core/fs，静态纯 record）：

- `Account(writes, flushes, soloCostNanos, batchedCostNanos)` 契约构造
 （0 ≤ flushes ≤ writes、有写必有刷——一刷多写是合并、一写多刷是浪费）；
- `amortizationRatio()` 摊薄倍数（零写 -1 哨兵）；
- `savedFlushes()` 省刷数 + `savingsNanos()` 时延净省（**可为负**——
  合并不划算面诚实可判）+ `savingsRatio()` 节省率（零分母 -1 哨兵）。

## User Stories

1. 作为写路径调优者，摊薄 5 倍+节省率 0.7 → 组提交显著划算，等批窗可
   再拉长；净省为负 → 该缩窗或关。
2. 作为容量审计者，savedFlushes×刷盘单价 = 设备寿命节省（fsync 次数
   即 SSD 磨损）。
3. 作为框架宿主，成本口径自声明，纯记账零合并。

## Implementation Decisions

- 纯记账不合并（刷盘策略归宿主）；「一写多刷是浪费不入账」的契约面
  显式入档。

## Testing Decisions

- 摊薄/省刷/净省/比率四读数；不划算面负值诚实；零写哨兵；畸形四型
  fail-fast。

## Out of Scope

- 不实现合并窗口；不做动态窗宽（IO 压力自适应归未来静脉）。

## Further Notes

- 与 RollingJsonlWriter 正交：那是滚动写本体，这是合并收益账面。
