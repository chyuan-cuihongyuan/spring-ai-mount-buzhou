# Spec 1827 — 优雅停机排空预测（effort #1827，R28）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2855–T2856，impl 1428）。借鉴：
> k8s drain / Envoy shutdown drain——停机超时来自排空负载的 makespan 预测，
> 不是拍脑袋常数。

## Problem Statement

优雅停机超时到处是魔法数：设短了排空不完强杀（工作丢失），设长了发布
拖尾——「还剩多少工作、以当前并行度多久排完、谁拖后腿」没有预测面。

## Solution

`DrainForecast`（core/session，静态纯函数）：

- `SessionWork(sessionId, remainingUnits)` 单会话剩余（契约 id 非空白、
  remaining ≥ 0）；
- `forecast(parallelism, millisPerUnit, work)` → `Forecast(sessions,
  totalRemainingUnits, makespanMillis, bottleneckSession, parallelismBound)`：
  makespan 单位 = max(最大单会话剩余, ceil(总剩余÷并行度))，换算毫秒；
  并行度主导以 ceil 商**严格大于**最大单会话为准（相等即单会话主导——
  并列取更可操作的处方：催单点）。

## User Stories

1. 作为发布脚本作者，预测 21ms×缩放系数 → 停机超时 = 预测 × 安全余量，
   不再拍常数。
2. 作为值班者，parallelismBound=true → 加并行或延窗；=false → 瓶颈会话
  单上催（bottleneck 直读）。
3. 作为框架宿主，工作单位口径（轮次/字节数）自声明，纯预测零执行。

## Implementation Decisions

- 纯预测不排程（排空执行归宿主）；ceil 除法保证整数并行语义。
- fail-fast：并行度 < 1、负耗时、空白 id、负剩余；null 按空表。

## Testing Decisions

- 单会话主导（胖子 1000 拖死）；并行度主导（41÷2 ceil 21 > 11）；相等
  取单会话主导；空/null 零预测；畸形四型 fail-fast。

## Out of Scope

- 不执行排空；不做会话优先级排序（先催谁归策略层）。

## Further Notes

- 与 SessionHibernationPolicy 正交：那是闲置足迹，这是停机工作预测。
