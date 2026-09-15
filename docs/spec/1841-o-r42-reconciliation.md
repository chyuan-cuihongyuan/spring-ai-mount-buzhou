# Spec 1841 — O 系 R42 对账轮（effort #1841，R42）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2883–T2884，impl 1442）。
> 对账口径延续七波惯例：快照补登前置 + README 先落 + 全仓 clean verify
> + 台账四断言。

## Problem Statement

O 系已落 41 轮，需第七次周期核账：R37–R41 五个新公共类型快照补登、三门
全绿、Wave 8 排程落图。

## Solution

R42 对账轮：快照补登前置（五类型）+ README 先落 + 全仓 clean verify +
台账四断言 + Wave 8 落图。

## User Stories

1. 作为 O 会话驾驶者，R42 后 Wave 8 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第七波）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题。

## Further Notes

- 共享检出特性入档：并行会话 push 会携带本地全部提交（R40 期实证），
  GitHub 中断期积压可被并行 push 顺带补推。
