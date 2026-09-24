# Spec 5040 — Blocked Sliding Counter 分块滑窗计数器（effort #5040，S41）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6181–T6182，impl 2191）。
> 借鉴：Datar-Gionis 指数直方图同族的分块近似面（√N 分块滑窗思想）。
> **勘误**：本件原拟 DG 指数直方图（ExponentialHistogram）——
> 推演中桶端点/过期语义与误差界自洽性不可达（Python 预演
> 违界），按诚实边界换为构造上可证正确的分块面（误差界
> 显式 ≤ 一个块计数），指数粒度面留雾区。

## Problem Statement

滑窗计数的病：每事件一个计数的精确滑窗（内存随窗口
线性爆）或无界衰减估计（窗口界不可知）——**有界内存
+显式误差界面**缺失。

## Solution

`BlockedSlidingCounter`（core/metrics）：

- 窗口切 m 块（块长 B=⌈N/m⌉），每块精确计数、整块进
  整块出（完全出窗才淘汰）；
- `lowerEstimate`=Σ块计数−最旧块计数（最旧块整块扣除）、
  `upperBound`=Σ块计数——**真值恒在两界之间**（最旧块只
  知部分在窗），界宽 ≤ 一个块计数 ≤ B，内存 O(m)；
- 读数：position/blockCount/blockSize；
- fail-fast：windowSize<1、blockCount<1。

## User Stories

1. 作为监控作者，500 位扫描真值逐位落界——估计可信。
2. 作为容量作者，blockCount 读数——内存上界恒定可见。

## Testing Decisions

- 小窗逐位钉住（N=10/B=5，位 11：[2,3] 含真值 3，块数 3）；
  500 位扫描（周期 7 命中，窗 100/块 10）：每位暴力真值
  ∈[下界,上界] 且界宽 ≤10；零流恒零；参数 fail-fast。

## Out of Scope

- 不做指数粒度桶（DG 指数直方图原面留雾区）；不做分位数
  （tdigest 面已有）；不做多键路由。

## Further Notes

- 与 SlidingWindowCounter（限流滑窗）同族不同面：固定单位
  桶限流计数 vs 任意事件流近似计数+显式误差界。Wave 7
  第五件。
- 里程碑：S41/50（82%）。
