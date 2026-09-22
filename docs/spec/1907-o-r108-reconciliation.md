# Spec 1907 — O 系 R108 对账轮（effort #1907，R108）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3015–T3016，impl 1508）。
> 对账口径延续十八波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 19 排程落图。

## Problem Statement

O 系已落 107 轮，需第十八次周期核账：R103–R107 五个新公共类型快照
补登、三门全绿、Wave 19 排程落图。

## Solution

R108 对账轮：快照补登（五类型 1111→1116）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 19 落图。

## User Stories

1. 作为 O 会话驾驶者，R108 后 Wave 19 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十八波）；verify 沿用非 clean
  离线口径（R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿；外域
  摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- Wave 18 撞坑两连（环形轮转→SmoothWeightedSequence 占坑、快速
  重传→FastRetransmitTrigger 占坑）换静脉成功。
