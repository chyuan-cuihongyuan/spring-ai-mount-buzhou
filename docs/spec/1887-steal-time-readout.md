# Spec 1887 — 窃取时间读面（effort #1887，R88）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2975–T2976，impl 1488）。借鉴：
> Linux /proc/stat 的 steal time——虚拟机 CPU tick 被宿主挪给其他
> 租户的账（st 字段）：应用「自己没干活但也没闲着」时唯一可见的
> 争用信号。

## Problem Statement

虚拟化/容器环境里 CPU 饱和读数失真：宿主超卖时应用的 user/system
占比双双下降但工作没变快——idle 看似充足实际 tick 被偷走；steal
占比没有独立读面时，容量排查在「CPU 不忙但慢」的死胡同里打转。

## Solution

`StealTimeReadout`（core/metrics，静态纯函数）：

- `stealRatio(prevSteal, currSteal, prevTotal, prevCurr...)`：
  Δsteal/Δtotal——两采样点的窃取占比（total 为全字段 tick 和）；
- `isContended(ratio, threshold)`：占比 ≥ 阈值 → 争用判定。

## User Stories

1. 作为容量排查者，Δsteal 60 / Δtotal 1000 = 6% > 5% 阈值 → 争用
   实锤——「不忙但慢」有解。
2. 作为监控作者，占比读数接入告警面——宿主超卖可见。
3. 作为诚实口径者，Δtotal=0（未流逝）哨兵 0.0 而非除零。

## Implementation Decisions

- 纯函数零状态；计数单调（curr ≥ prev）fail-fast；Δtotal=0 哨兵
  0.0；阈值 ≥ 0 fail-fast。

## Testing Decisions

- 经典 Δ60/Δ1000=6% 一例；阈值两侧行为；零流逝哨兵；畸形四型
  （计数倒退/total 倒退/负阈值）fail-fast。

## Out of Scope

- 不做 tick 采集（/proc 读取归宿主）；不做宿主迁移建议。

## Further Notes

- 与 PSI 压力失速读面（#1801 some/full）互补：PSI 是任务在等什么，
  steal 是 tick 被谁拿走。
