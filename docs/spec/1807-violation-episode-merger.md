# Spec 1807 — 追限事件会话化合并（effort #1807，R8）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2815–T2816，impl 1408）。借鉴：
> Prometheus 告警分组（group_interval 相邻同类告警并一条）/ Google Analytics
> session gap（无活动超窗即切会话）——会话化（sessionization）。

## Problem Statement

限流计数只有累计值：10 次散点追限与 1 段 10 连击在裸计数里同形——前者是
偶发抖动（加余量即可）、后者是系统性超载（该扩容或降级），处置相反却无
读数可分。

## Solution

`ViolationEpisodeMerger`（core/ratelimit，静态纯函数）：

- `merge(gapToleranceMillis, violationAtMillis)` → `MergeReport(episodes,
  totalHits, longest, mergedSpans)`；
- 语义：时点排序后相邻差 ≤ 容忍窗并入同**事件段**（Episode：start/end/
  hits/span）；单点成段（span 0）；
- `hitsPerEpisode()` 平均段密度（无事件 -1 哨兵）——越高越接近持续超载。

## User Stories

1. 作为限流治理者，episodes=2、hitsPerEpisode=5 → 少段高密度 = 持续超载，
   该扩容；episodes=10、密度 1 → 散点抖动，加余量就够。
2. 作为值班者，longest 段的 span/hits 直接进事件报告——「最长连续追限
   80ms 内 3 次」比「累计 5 次」可读。
3. 作为框架宿主，时点口径自声明、乱序容忍（内部排序），纯读面零侵入。

## Implementation Decisions

- 纯读面零状态，不动限流判定；只读不裁决。
- 乱序容忍（排序后切会话）；契约 fail-fast：负容忍窗/null 时点；null 列表
  按空表。

## Testing Decisions

- 并段/切段；乱序同语义；单点零 span 段；空表/null 哨兵；畸形 fail-fast。

## Out of Scope

- 不做告警通知动作；不接指标导出。

## Further Notes

- 与 TurnRateLimitHook 正交：那是限流动作，这是事件流形状读数。
