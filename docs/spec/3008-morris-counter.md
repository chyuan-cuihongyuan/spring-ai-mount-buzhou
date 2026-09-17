# Spec 3008 — Morris 近似计数器（effort #3008，R9）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5017–T5018，impl 2009）。
> 借鉴：Robert Morris 1978 概率计数——log log n 空间近似计数。

## Problem Statement

海量低价值计数（调用次数/心跳数/提及数）每个精确 long 计数都写
共享状态——写放大与空间随基数线性涨；这类口径只需要数量级正确的
粗估计。

## Solution

`MorrisCounter`（core/metrics）：

- 只存指数 v：`increment()` 以 2^−v 概率才 v+1（计数越大越懒；
  v=0 必增——首计数不丢）；
- `estimate()` = 2^v − 1（期望无偏 E[estimate]=n）；v=0 → 0 诚实零；
- 指数封顶 62（double 概率分辨率与 long 饱和的诚实边界，覆盖
  ~4.6e18）；`rawExponent()` 对账面；`reset()`；RandomGenerator
  注入确定性回放。

## User Stories

1. 作为指标作者，万级低价值计数各占一个 int 指数——空间 O(log log n)。
2. 作为热点作者，计数越热写越懒——写放大随计数对数衰减。

## Testing Decisions

- 零态 0；首计数必中（v=0 概率 1）；500 件×64 增量均值 ∈
  [0.8n, 1.25n]（无偏的系综证据）；2000 增量估计单调不降；1000
  增量指数 ≤15（亚对数空间主张）；归零；同种子轨迹回放。

## Out of Scope

- 不做多计数共享随机流（每计数独立件）；不做泛化基数 b（Morris+
  /Morris++ 变体留白）；不接 metrics 导出。

## Further Notes

- 与 HyperLogLog（基数去重）/ FrequencySketch（频率素描）互补：
  本件是**单流总计数**的近似件，不识独立元素。
- 里程碑：9/150。
