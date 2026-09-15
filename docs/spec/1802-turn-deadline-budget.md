# Spec 1802 — 轮墙钟预算传播裁决（effort #1802，R3）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2805–T2806，impl 1403）。借鉴：
> gRPC deadline propagation（预算沿调用链传播递减、min 截断规则、DEADLINE_
> EXCEEDED 不起工）/ Temporal schedule-to-close（整体关闭超时而非逐活动独立）。

## Problem Statement

一轮对话内的多个工具调用各自独立计超时：单次不超时但串起来把轮墙钟拖成 N
倍；预算快耗尽时新调用照常起工、注定做一半被砍——既浪费工又拉长尾延迟。
现状（ToolTimeoutOverrides 是逐工具覆盖面）没有「轮级预算如何传播到逐调用」
的裁决语义。

## Solution

`TurnDeadlineBudget`（core/exec，静态纯函数）：

- `plan(totalNanos, estimatedNanos)` → `BudgetPlan(totalNanos, allocations,
  committedNanos)`：逐调用 `CallAllocation(callIndex, admitted,
  effectiveTimeoutNanos)`；
- 语义三规则：剩余预算顺序递减（传播）；获准超时 = min(预估, 剩余)（截断）；
  剩余 = 0 → 拒绝（零耗预估也不例外——deadline 先于派发检查）；
- `admissionRatio()` 获准率（空计划 -1 哨兵）。

## User Stories

1. 作为 Agent 宿主，轮预算 100ms、四调用预估 30/50/40/10 → 计划直接给出
   30/50/20/拒——第三调用截断保命、第四调用不起工。
2. 作为延迟治理者，admissionRatio 常年 <1 说明预算与工具耗时分布不匹配，
   该调预算该换工具。
3. 作为框架使用者，预估口径自声明（P50 历史/静态配置），裁决纯函数可单测
   可回放。

## Implementation Decisions

- 纯裁决不执行（派发归宿主）；core/exec 与既有工具执行脊柱同包。
- 契约 fail-fast：负预算/负预估 IllegalArgumentException；null 按空表。

## Testing Decisions

- 截断+耗尽拒绝+获准率；前缀性（早花光全拒）；零预算拒绝一切（含零耗）；
  空表/null 哨兵；畸形 fail-fast。行为断言，不测内部。

## Out of Scope

- 不接 HarnessToolCallingManager 热路径（接线归后续独立轮）；
- 不做预算动态再协商（budget 沿程只减不增）。

## Further Notes

- 与 ToolTimeoutOverrides 正交：那是覆盖工具自身超时；这是轮级传播裁决。
