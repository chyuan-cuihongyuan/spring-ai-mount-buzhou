# Spec 1847 — O 系 R48 对账轮（effort #1847，R48）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2895–T2896，impl 1448）。
> 对账口径延续八波惯例：快照补登前置 + README 先落 + 全仓 clean verify
> + 台账四断言。

## Problem Statement

O 系已落 47 轮，需第八次周期核账：R43–R47 五个新公共类型快照补登、三门
全绿、Wave 9 排程落图。

## Solution

R48 对账轮：快照补登前置（五类型）+ README 先落 + 全仓 clean verify +
台账四断言 + Wave 9 落图。

## User Stories

1. 作为 O 会话驾驶者，R48 后 Wave 9 无欠账带入（约 1/3 里程碑）。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第八波）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题。

## Further Notes

- 八波零缺陷通过（R6 抢救三连环、R30 K 双病理为跨会话就近处置非本系
  欠账）。
