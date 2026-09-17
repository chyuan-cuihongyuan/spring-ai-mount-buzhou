# Spec 3006 — P² 流式分位数（effort #3006，R7）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5013–T5014，impl 2007）。
> 借鉴：Jain-Chlamtac P² 算法——五标记点增量分位估计。

## Problem Statement

流式分位数（延迟 p50/p99）精确口径需存全样本 O(n) 空间；排序缓冲
窗口又受窗口大小限制。O(1) 空间的在线分位估计缺一个地基件。

## Solution

`PSquareQuantile`（core/metrics，单流口径）：

- 五标记（min / p/2 / p / (1+p)/2 / max）位置账面 + 理想位
  n'[i]=1+(count−1)·p_i；
- 每样本：cell 定位（越界两端直接改写端标记）→ 位置递增 →
  内部标记 1..3 每步 ±1 逼近（**抛物线三点拟合**主路径，越序
  退线性插值保标记有序）；
- estimate：初始化后即中标记；count<5 排序缓冲下标诚实口径；
  count=0 NaN；p∈(0,1) 开区间校验。

## User Stories

1. 作为指标作者，延迟 p99 单遍 O(1) 空间流式估计——免全样本留存。
2. 作为评估作者，p 任意（0.25/0.5/0.9…）同一件通吃。

## Testing Decisions

- 洗牌整数流（种子确定性）：p50 → 499.5±5、p90 → 899.1±10；
- 常量流精确 7.0；单调升 1..1000 → 500±10（端标记改写+逐步逼近
  的 torture）；前 5 样本排序缓冲口径（{30,10,20} p50 → 20）；
  空态 NaN；p 开区间四路 fail-fast；重复值密集流标记有序。

## Out of Scope

- 不做多分位同流并行（多 p 各建一件即可）；不做误差界证明
  （光滑分布 O(1/n) 经验口径）；不接 metrics 导出。

## Further Notes

- 与 FiveNumberSummary（全量精确）/ WelfordAccumulator（矩）成
  分布刻画三件：全量五点 / 流式矩 / 流式分位。
- 里程碑：7/150。
