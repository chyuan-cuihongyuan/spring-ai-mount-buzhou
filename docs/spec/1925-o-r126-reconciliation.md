# Spec 1925 — O 系 R126 对账轮（effort #1925，R126）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3051–T3052，impl 1526）。
> 对账口径延续二十一波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 22 排程落图。

## Problem Statement

O 系已落 125 轮，需第二十一次周期核账：R121–R125 五个新公共类型
快照补登、三门全绿、Wave 22 排程落图；本会话（O 系续推）至此完成
50 轮（R77–R126）。

## Solution

R126 对账轮：快照补登（五类型 1126→1133）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 22 落图。

## User Stories

1. 作为 O 会话驾驶者，R126 后 Wave 22 无欠账带入，本会话 50 轮
   达成。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第二十一波）；verify 沿用非
  clean 离线口径（R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿；外域
  摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- Wave 21 弹性位（复合健康分/时钟抖动测量）落位；50 轮里程碑
  （R77–R126，含 8 个对账轮与 42 个特性轮中的本会话部分）。
