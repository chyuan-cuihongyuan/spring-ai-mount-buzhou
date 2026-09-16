# Spec 2027 — EWMA 估计器（effort #2027，R28）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3155–T3156，impl 1578）。
> 借鉴：Netflix/Finagle 指标平滑口径——指数加权移动平均。

## Problem Statement

原始指标（延迟/速率/误差比）逐样本抖动剧烈：瞬时尖峰触发伪告警、
逐点图不可读——观测需要「跟得上趋势、滤得掉尖峰」的平滑层。

## Solution

`EwmaEstimator`（core/metrics，单写者口径非线程安全——多线程每线程
一件或外同步）：

- `observe(value)`：首样本**直接锚定**（不从 0 爬坡），其后
  estimate = α×新值 + (1−α)×旧值；
- α ∈ (0,1]（默认 0.2 ≈ 5 样本记忆）：1 直通最新、小 α 惯性大；
- 读数：estimate()（未观测 NaN 配 hasSamples()）/ observations() /
  reset()；
- 契约：value 有限非 NaN、α ∈ (0,1] fail-fast。

## User Stories

1. 作为指标作者，EWMA 层让延迟读数可读——尖峰被稀释趋势仍跟随。
2. 作为告警调参者，α 旋钮调灵敏度——大 α 快响应小 α 强平滑。

## Testing Decisions

- 首样本锚定；α=0.5 二样本 150 精确；α=1 直通；α=0.1 尖峰稀释
  （1000 观测后 190）；50 期收敛（容差 0.01——残余 (1−α)^n 数学口径）；
  单调输入估计单调；reset 回未锚定；畸形四型 fail-fast。

## Out of Scope

- 不做时钟衰减（时间维 EWMA 归指数直方图族）；不接 metrics 桥。

## Further Notes

- 与 MinRttTracker（最小值基线）对照：EWMA 平滑均值域，min-RTT 下界域。
