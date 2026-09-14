# 1617 · 梯度式自适应并发闸（Netflix Gradient2 思想）

> 来源：N 会话 R18（effort #1617 / T2385–T2386 / impl 1170）。借鉴对象：Netflix
> concurrency-limits Gradient2 / Envoy adaptive_concurrency——以延迟梯度（而非失败）
> 驱动并发上限：过载的前兆是延迟爬升，等到失败已经晚了。

## Problem Statement

自适应隔舱（spec 145）是失败驱动 AIMD：只有失败发生才减并发——第一波失败
已经付出代价。梯度算法看延迟：近期延迟相对基线劣化即下调（预防），明显变快
即缓慢上调（探测余量）。

## Solution

`GradientAdaptiveLimiter`（core/concurrent，与 AdaptiveBulkhead 正交并存）：

- 双 EMA：recent（α=0.4 快响应）/ baseline（α=0.05 慢基线）；
  gradient = baseline / recent（>1 变快、<1 劣化）。
- 调整语义（钉住）：warmup 8 样本只学习；gradient ≥ 1+tolerance → limit+1
  （封顶 maxLimit）；≤ 1−tolerance → limit×gradient 乘性下调（地板 minLimit，
  每步至少 -1）；容错带内不动（tolerance 默认 20% 防抖）。
- 无等待语义：tryAcquire 超动态上限 fail-fast（信号质量优先）。
- 观测：View(limit/inFlight/两 EMA/gradient/上下调次数)。

## User Stories

1. 作为运维者，我想让过载在失败前被规避，所以延迟爬升即下调并发。
2. 作为运维者，我想让余量被持续探测，所以变快时缓慢升并发。
3. 作为开发者，我想语义可测，所以容错带/warmup/上下调计数全部有断言。

## Testing Decisions

- `GradientAdaptiveLimiterTest` 七断言：劣化 3 倍乘性下调（先升脱离 minLimit
  再断降——零失败发生）；变快 5 倍加性上涨 + gradient>1.2；50% 容差带内 30%
  劣化不动；warmup 只学不调（EMA 已收敛、调整计数零）；tryAcquire 联动动态
  上限 + release 复用；持续极快封顶 maxLimit；非法配置 fail-fast。

## Out of Scope

- 与 AdaptiveBulkhead 的装配二选一面（宿主按负载特征选型——文档说明即可）。
- percent-based 窗口（EMA 已足够——钉住系数，换窗是行为变更需独立 spec）。

## Further Notes

- 钉住的混合策略（加性升/乘性降）与 Netflix 实现同工程取向：上升保守、下降果断。
