# Spec 3013 — 扫线最大并发（effort #3013，R14）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5027–T5028，impl 2014）。
> 借鉴：计算几何扫线（事件点计数）。

## Problem Statement

「某时刻最多多少会话同时在档 / 工具调用在途 / 配额被占」若按逐
时刻采样或两两区间判交，要么粗要么 O(n²)；闭区间口径还会把相邻
相接段重复计数。

## Solution

`SweepLineIntervals`（core/metrics，纯函数静态件）：

- 半开区间 `[start,end)`：起 +1 / 止 −1 事件，按时间排，
  **同刻 −1 先于 +1**（相接不算并发）；
- `SweepResult(maxConcurrent, atPoint, intervals)`——峰值并发 +
  首发点（同峰取最早）+ 区间总数；
- 空集诚实（0 / NO_PEAK_POINT 哨兵 / 0）；零宽 [x,x) 贡献零
  （合法空占位）；start>end / null fail-fast。

## User Stories

1. 作为容量作者，历史区间一次扫线得并发峰值——容量规划有据。
2. 作为对账作者，峰值点可指认（哪个时刻开始到顶）。

## Testing Decisions

- [0,10)+[5,15)+[12,20) 峰 2@5 手算；[0,5)+[5,10) 恒 1（半开语义
  ——闭区间误报 2 的对照）；三层嵌套峰 3@4；全不相交峰 1@最早
  起点；空集哨兵；零宽贡献零；start>end/null fail-fast；乱序输入
  结果不变；双峰并列取最早首发点。

## Out of Scope

- 不做滑窗时间桶近似（精确单遍已够）；不做带权计数（每区间权重
  留白）；不接 metrics 导出。

## Further Notes

- 与并发组闸/舱壁读数互补：那些是**在途限制**，本件是**历史峰值
  度量**。
- 里程碑：14/150。
