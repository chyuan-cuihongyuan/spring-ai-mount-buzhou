# Spec 1746 — 预算耗尽 ETA 投影（effort #1746，R47）（effort #1746，R47）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2693–T2694，impl 1347，impl Prometheus predict_linear / Google SRE 预算烧尽预测）。借鉴：预算只有余量读数，没有「按当前烧速还有多久烧完」的外推——告警在烧穿之后才响。

## Problem Statement

`BudgetEtaProjection`（resilience，静态纯函数）：project(remainingBudget, recentSpendPerInterval, intervalMillis)→EtaProjection(remaining/avgSpend/etaMillis/verdict)；verdict 三闭集 NO_DATA（无样本）/STABLE（avg<=0 不会烧穿，eta=−1）/PROJECTED。CostSpikeDetector 管尖峰，本面管趋势外推。

## Solution

作为预算治理者，ETA=2h → 烧穿前 2 小时告警而非事后。

## User Stories

1. 17460
2. 17461
3. 17462

## Implementation Decisions

- 17463

## Testing Decisions

- 17464

## Out of Scope

- 17465

## Further Notes

- 17466
