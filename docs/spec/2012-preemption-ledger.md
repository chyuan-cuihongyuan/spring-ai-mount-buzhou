# Spec 2012 — 抢占重算账本（effort #2012，R13）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3125–T3126，impl 1563）。
> 借鉴：vLLM preemption/recompute——抢占不是免费的，双面账定去留。

## Problem Statement

高优先级任务抢占低优先级（spawn 优先级调度已有），但抢占的代价无人
记账：victim 已做工作作废（重算浪费）、抢占方提前完成（等待节省）——
净收益不明时，抢占阈值只能拍脑袋。

## Solution

`PreemptionLedger`（core/exec，synchronized 小临界区）：

- `recordPreemption(victimId, workTicks, savedTicks)`：一笔抢占双面入
  账（浪费面/收益面）；
- `recordRecomputation(victimId)`：victim 重算完成（幂等——同一
  victim 一次）；
- 读数四件：`stats()`（抢占数/重算数/浪费/节省/净收益）、
  `netBenefitTicks()`（节省−浪费，负即降阈值信号）、`wasteRatio()`
  （浪费占比）、`recomputeRate()`（重算发生率——0 即 victim 全被
  放弃）；
- 空账不除零；契约：ticks ≥ 0、id 非空 fail-fast。

## User Stories

1. 作为调度作者，netBenefit 持续为负 → 抢占在赔——降抢占阈值有据。
2. 作为 SRE，wasteRatio = 抢占的健康度——升即抢占质量恶化。

## Testing Decisions

- 单笔双面账；多笔累计净负态；重算幂等（同 victim 一次）；空账零
  不除零；净收益正/负/零三态；畸形四型 fail-fast。

## Out of Scope

- 不接 SpawnGate 调度链（接线归后续轮）；不做按时间窗的滚动账（累计
  账口径）。

## Further Notes

- 与 SpawnRejectionDistribution（拒绝分布）正交：拒绝记门前，抢占记
  门前让位后的代价。
