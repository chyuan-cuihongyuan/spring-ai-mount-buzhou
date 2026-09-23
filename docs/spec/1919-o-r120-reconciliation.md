# Spec 1919 — O 系 R120 对账轮（effort #1919，R120）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3039–T3040，impl 1520）。
> 对账口径延续二十波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 21 排程落图。

## Problem Statement

O 系已落 119 轮，需第二十次周期核账：R115–R119 五个新公共类型
快照补登、三门全绿、Wave 21 排程落图。

## Solution

R120 对账轮：快照补登（五类型 1121→1126）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 21 落图。

## User Stories

1. 作为 O 会话驾驶者，R120 后 Wave 21 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第二十波）；verify 沿用非 clean
  离线口径（R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿；外域
  摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- Wave 20 撞坑一连（半衰期→FactDecayPolicy 占坑换写放大读面）；
  网络断续期推送批量化处置常态化。
