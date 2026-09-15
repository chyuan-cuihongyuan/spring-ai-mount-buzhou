# Spec 1835 — O 系 R36 对账轮（effort #1835，R36）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2871–T2872，impl 1436）。
> 对账口径延续 R6/R12/R18/R24/R30：快照补登前置 + README 先落 + 全仓
> clean verify + 台账四断言。

## Problem Statement

O 系已落 35 轮，需第六次周期核账：R31–R35 五个新公共类型快照补登、三门
全绿、Wave 7 排程落图。

## Solution

R36 对账轮：快照补登前置（五类型）+ README 先落 + 全仓 clean verify
（一次过绿目标）+ 台账四断言 + Wave 7 落图。

## User Stories

1. 作为 O 会话驾驶者，R36 后 Wave 7 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题。

## Further Notes

- 六波节奏稳定（每 6 轮一対账，快照前置后均一次过绿——R12/R24/R30/R36）。
