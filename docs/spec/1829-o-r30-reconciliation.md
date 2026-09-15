# Spec 1829 — O 系 R30 对账轮（effort #1829，R30）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2859–T2860，impl 1430）。
> 对账口径延续 R6/R12/R18/R24：快照补登前置 + README 行先落 + 全仓 clean
> verify + 台账四断言。

## Problem Statement

O 系已落 29 轮，需第五次周期核账：R25–R29 五个新公共类型快照补登、三门
全绿、GitHub 第三次中断期积压（R28/R29）待恢复补推、Wave 6 排程落图。

## Solution

R30 对账轮四件事：快照补登前置（五类型）；全仓 clean verify（一次过绿
目标）；台账四断言；补推确认。

## User Stories

1. 作为 O 会话驾驶者，R30 后 Wave 6 无欠账带入（1/5 里程碑达成）。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（快照前置/README 先落/一次过绿）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题。

## Further Notes

- GitHub 三次中断模式（约每 8–10 轮一次，每次 2–3 轮时长），本地积压+
恢复即推策略三次验证有效。
