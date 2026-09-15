# Spec 1808 — 驱逐信号阈值门（effort #1808，R9）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2817–T2818，impl 1409）。借鉴：
> K8s eviction manager——驱逐信号分硬阈值（立即逐）与软阈值+宽限期
> （宽限满仍在线才逐），两级阈值避免一刀切在临界点抖动驱逐。

## Problem Statement

Spill 逐出（EvictHandleTool 手动）没有自动阈值语义：单一阈值要么太灵敏
（临界点反复逐/回读抖动）要么太迟钝（盘满才动手）；「软阈先警告给宿主
自救窗、硬阈保命立即逐」的两级裁决无处安放。

## Solution

`EvictionThresholdGate`（buzhou-spill，静态纯函数）：

- `Thresholds(soft, hard)` 阈值对（构造器核契约：0 ≤ soft ≤ hard 非 NaN）；
- `decide(signal, thresholds, millisAboveSoft, graceMillis)` → 三态
  `BELOW / GRACE_PENDING / EVICT_NOW`：signal ≥ hard 立即逐（宽限不豁免）；
  signal ≥ soft 看宽限（millisAboveSoft ≥ grace 即逐，否则待观察）；
- `census(thresholds, grace, samples)` 多信号批量三态普查
  （DecisionCensus + evictRatio，空 -1 哨兵）。

## User Stories

1. 作为 spill 治理者，配额占用 0.75（软阈 0.7 硬阈 0.9）→ GRACE_PENDING：
   有 1 秒自救窗自发回收；0.95 → EVICT_NOW 保命立即逐。
2. 作为值班者，census 一眼读出 5 信号中 2 立即逐 1 待观察——驱逐面有多大。
3. 作为框架宿主，信号口径（占用率/句柄数/水位）自声明，纯裁决零状态。

## Implementation Decisions

- 纯裁决不执行（驱逐动作归宿主）；K8s 语义保留「硬阈不受宽限豁免」。
- fail-fast：阈值倒挂/NaN、负毫秒、NaN 信号；null 样本按空表。

## Testing Decisions

- 三态裁决四情形；普查计数+占比；空普查哨兵；畸形四型 fail-fast。

## Out of Scope

- 不接 SpillQuota 热路径（接线归后续轮）；不做驱逐对象选择（最冷优先策略
  归宿主）。

## Further Notes

- 与 EvictHandleTool 正交：那是手动逐出工具，这是自动阈值裁决语义。
