# Spec 3034 — Croston 间歇需求预测器（effort #3034，R35）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5069–T5070，impl 2035）。
> 借鉴：Croston 1972（间歇需求分量子平滑）。

## Problem Statement

稀疏序列（大量零+偶发需求——工具调用/审计事件/故障上报）朴素
指数平滑被零海拖向零（偶发毛刺完全淹没）；Holt 也假设连续趋势。
间歇型序列缺一个「率」口径的预测件。

## Solution

`CrostonForecaster`（core/metrics，单流口径）：

- **双分量分开平滑**（仅非零观测更新）：需求大小 z' =
  αz+(1−α)z'；需求间隔 p' = αp+(1−α)p'；
- `forecastPerPeriod()` = z'/p'（每周期期望需求率——「毛刺多大
  不重要，率才是口径」）；首个非零直接初始化；
- 全零序列 NaN（诚实——零海无基准）；demand ≥ 0、α∈(0,1)
  校验；observations/nonzeroCount 稀疏度对账面。

## User Stories

1. 作为容量作者，稀疏事件流的每周期期望量——被零海淹没的信号
   重新可读。
2. 作为对账作者，稀疏度与双分量全读数——口径可审计。

## Testing Decisions

- 常数模式（周期 3 尺寸 5）60 轮收敛 5/3±0.1；全零 100 期恒
  NaN；首非零直接初始化（10/1=10）；率等价性（每 2 期 4 单 vs
  每 4 期 8 单——率同 ~2，形状不变性主张）；α=0.5 手算（10→0→8
  得 9/1.5=6.0）；计数对账；α/负需求四路 fail-fast。

## Out of Scope

- 不做 SBA/Wintles 偏差修正（Croston 变体族留白）；不做需求
 分类（零膨胀模型留白）；不接容量规划（接线归调用方）。

## Further Notes

- 与 EwmaEstimator（连续水平）/ HoltForecaster（连续趋势）成
  预测三件：连续水平 / 连续趋势 / 间歇率——按序列形态选型。
- 里程碑：35/150。
