# Spec 1720 — dry-run 决策分布（effort #1720，R21）（effort #1720，R21）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2641–T2642，impl 1320，impl Terraform plan 决策分布）。借鉴：DryRunHook 预演「真跑会怎样」，但预演结论无分布：多少会放行/多少会拦下/预演自身出错多少——dry-run 价值不可量度。

## Problem Statement

`DryRunDecisionStats`（core/exec，实例面线程安全）：Decision 闭集（WOULD_RUN/WOULD_BLOCK/PLAN_ERROR）+record+census→PlanCensus(planned/wouldRun/wouldBlock/planErrors/blockRatio 无样本 −1)+resetForTest。纯读面 opt-in。

## Solution

作为安全治理者，blockRatio=0 → dry-run 白开（没拦过任何东西）。

## User Stories

1. 17200
2. 17201
3. 17202

## Implementation Decisions

- 17203

## Testing Decisions

- 17204

## Out of Scope

- 17205

## Further Notes

- 17206
