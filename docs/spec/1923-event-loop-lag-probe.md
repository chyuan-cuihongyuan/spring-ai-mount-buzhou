# Spec 1923 — 事件循环滞后探针（effort #1923，R124）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3047–T3048，impl 1524）。借鉴：
> Node.js 事件循环滞后探针惯例——调度一个 0ms 定时器，实际执行
> 时刻与调度时刻之差即滞后：滞后 = 循环被同步任务/回调塞满的程度
> ——「调度器还灵不灵」的前置仪表。

## Problem Statement

调度器（虚拟线程池/定时器轮）饱和没有独立探针：任务延迟飙升被
归因于下游慢，实际是循环本身被塞满——调度时刻与执行时刻之差
（滞后）没有独立采样与判定面。

## Solution

`EventLoopLagProbe`（core/concurrent，静态纯函数）：

- `lagMillis(scheduledMillis, executedMillis)`：滞后 = executed −
  scheduled 与 0 取大（提前执行钳 0——定时器合并的负滞后无意义）；
- `verdict(lag, threshold)`：lag ≤ threshold → OK；否则 SATURATED
  （边界含上——恰在阈值内仍 OK）。

## User Stories

1. 作为调度器作者，调度 0ms 定时器实际 45ms 后执行 → 滞后 45ms
   ——循环塞满程度直读。
2. 作为告警作者，滞后 ≤ 10ms OK / > 10ms SATURATED——调度器
   饱和前置告警。
3. 作为口径诚实者，提前执行（负滞后）钳 0——负滞后非「健康」
   而是「时钟粒度噪声」。

## Implementation Decisions

- 纯函数零状态；scheduled/executed ≥ 0、threshold ≥ 0 fail-fast；
  负滞后钳 0。

## Testing Decisions

- 滞后两例（45/0）；判定两侧含上；负滞后钳 0；畸形两型（负时刻）
  fail-fast。

## Out of Scope

- 不做调度器实现（归执行器）；不做滞后根因定位。

## Further Notes

- 与 StealTimeReadout（tick 被宿主偷）互补：那是虚拟化层争用，
  这是进程内调度器饱和；与 PSI 读面互补：那是任务在等什么，这是
  调度器还转不转。
