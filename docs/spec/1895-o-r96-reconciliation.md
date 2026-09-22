# Spec 1895 — O 系 R96 对账轮（effort #1895，R96）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2991–T2992，impl 1496）。
> 对账口径延续十六波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 17 排程落图。

## Problem Statement

O 系已落 95 轮，需第十六次周期核账：R91–R95 五个新公共类型快照
补登、三门全绿、Wave 17 排程落图。

## Solution

R96 对账轮：快照补登（五类型 1101→1106）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 17 落图。

## User Stories

1. 作为 O 会话驾驶者，R96 后 Wave 17 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十六波）；verify 沿用非 clean
  离线口径（R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿；外域
  摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- Wave 16 五轮（阻塞期审计/分位聚合偏差/阶梯加压/幂等键判定/尾
  时延放大）全部首选题落位零换静脉。
