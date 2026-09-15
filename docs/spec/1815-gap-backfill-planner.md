# Spec 1815 — 缺口回填计划器（effort #1815，R16）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2831–T2832，impl 1416）。借鉴：
> Kafka offset 补填 / Prometheus backfill——停机断流后重放「不重不漏」的
> 前提是先知道缺哪段；缺口合并为区间而非逐点，回填可分批可并行。

## Problem Statement

会话事件序号在位集合与期望连续区间之差没有计划面：断流后该重放哪些段、
最大单段多长（重放瓶颈）、缺失率多高（完整性），靠人眼 diff 序号清单
既易漏又不可审计。

## Solution

`GapBackfillPlanner`（core/recovery，静态纯函数）：

- `plan(from, to, presentSeqs)` → `BackfillPlan(expectedCount, presentCount,
  gaps, largestGapSpan)`：缺口升序区间清单（含首尾缺口），在位乱序/重复
  容忍（内部排序去重）；
- `Gap(from, to)` 闭区间 + `span()`；
- `missingRatio()` 缺失率（期望 0 -1 哨兵）+ `complete()` 完整性判定；
- 契约 fail-fast：区间倒挂（from > to+1）、越界在位值、null 元素。

## User Stories

1. 作为恢复编排者，断流后缺口单 [(3,5),(8,8)] 直接变两批重放任务——分批
   并行不重不漏。
2. 作为审计者，largestGapSpan=4000 回答重放瓶颈单段长度；missingRatio
   回答完整性损失面。
3. 作为框架宿主，序号口径（事件 seq/轮号）自声明，纯计划零执行。

## Implementation Decisions

- 纯计划不执行（重放归宿主）；空区间用 from = to+1 表达（哨兵 -1）。
- 越界在位值 fail-fast（脏事实不吞）。

## Testing Decisions

- 中段缺口合并区间；首尾缺口入账；完整/全缺/乱序重复；空区间哨兵；畸形
  三型 fail-fast。

## Out of Scope

- 不执行重放；不做并行度决策（分批数≠并行度）。

## Further Notes

- 与 CheckpointLagReadout 配对：那是「尾部水位差」，这是「区间内空洞清单」。
