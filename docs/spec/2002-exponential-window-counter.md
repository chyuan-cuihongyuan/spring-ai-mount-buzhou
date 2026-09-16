# Spec 2002 — 指数直方图滑窗计数（effort #2002，R3）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3105–T3106，impl 1553）。
> 借鉴：Datar-Indyk-Lewkowitz-Rubinfeld 指数直方图——有界误差滑窗
> 聚合，O(log N) 空间。

## Problem Statement

滑动窗口内事件计数（最近 W tick 的错误数 / 限流命中数），精确做法要
存 O(W) 个时间戳。长窗口高频事件下内存线性涨；跨实例合并更无解。

## Solution

`ExponentialWindowCounter`（core/metrics，synchronized 小临界区）：

- 构造契约：window ≥ 1（fail-fast，以离散 tick 计）；
- 桶结构：(capacity, firstTimestamp, lastTimestamp)——同容量至多 2 桶，
  出现第 3 个即合并最老两个（容量翻倍、跨度并接），set 回最老原位保持
  last 非降序；
- `insert()` 记事件于当前 tick；`tick()` 时间推进一步；
- 整体过期（last ≤ now−window）桶惰性清出；
- `estimate()`：全界内桶（first > now−window）计全 + 跨界桶（first ≤
  now−window < last）计 floor(capacity/2)——事件分布未知的无偏假设；
- `errorBound()` = Σ跨界桶 ceil(capacity/2)（无跨界即 0——估计精确）；
- `bucketCount()` 空间读数（O(log N) 增长证）。

## User Stories

1. 作为观测作者，长窗（千 tick）高频事件计数只花 ≤24 个桶，误差自
   描述可告警。
2. 作为读数审计者，无跨界时估计精确——新鲜窗口不被近似污染。

## Implementation Decisions

- 桶存 first/last 双端口径（工程改良——经典 EH 只存 last 无法精确判
  跨界）；确定性（无随机数，同序列同答案可回放）。

## Testing Decisions

- 无跨界精确；滑出清零；burst+滑出；确定性序列误差 ≤ 自描述界
  （独立精确重放对照）；千事件桶数 ≤24 对数增长；畸形 fail-fast；
  交替 insert/tick 不丢新鲜事件。

## Out of Scope

- 不做带权事件（weight 合并语义留白）；不接 metrics 桥（后续轮）。

## Further Notes

- 与 RollingMaxCounter（精确窗）对照：精确 vs 省空间近似；与 HLL
  （基数）互补：窗口计数 vs distinct 计数。
