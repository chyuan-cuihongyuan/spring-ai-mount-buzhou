# Spec 1834 — 完成度 ETA 投影（effort #1834，R35）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2869–T2870，impl 1435）。借鉴：
> CI 进度条 / 带宽估计——rate = done/elapsed，ETA = remaining/rate 线性
> 外推；无基准不编速率。

## Problem Statement

长任务（评估 run/批量回放/导入）只有 done/total 静态计数：「还要多久」
靠人肉心算；进度条没有 ETA、投影总时长无从对比实际（尾段速率漂移靠
肉眼）。

## Solution

`EtaProjection`（core/eval，静态纯函数）：

- `estimate(doneUnits, totalUnits, elapsedMillis)` → `Projection(progress,
  etaMillis, projectedTotalMillis)`：线性外推；除不尽向上取整（保守——
  宁可多等不可少报）；
- 哨兵：done=0 或 elapsed=0（无速率基准）→ ETA/投影 -1（诚实——不编
  速率）；done=total → ETA 0、投影=elapsed；
- 契约 fail-fast：total < 1、done 越界、负 elapsed。

## User Stories

1. 作为评估运维者，10/40 用 100ms → ETA 300ms 直接进进度输出，不等
   人肉心算。
2. 作为容量规划者，多时点投影对比发现尾段漂移（投影前松后紧=速率
   不均匀，尾项贵）。
3. 作为框架宿主，单位与时钟口径自声明，纯投影零采样。

## Implementation Decisions

- 纯投影零采样（时钟归宿主）；无基准 -1 哨兵（比编造 0 速率诚实）。

## Testing Decisions

- 线性外推；无基准哨兵两型；已完成边界；取整保守；畸形三型 fail-fast。

## Out of Scope

- 不做加权滑动速率（EWMA 归未来静脉）；不采样时钟。

## Further Notes

- 与 EvalRunProgress 配对：那是计数快照，这是时间投影。
