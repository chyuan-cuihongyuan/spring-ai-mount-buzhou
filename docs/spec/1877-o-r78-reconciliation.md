# Spec 1877 — O 系 R78 对账轮（effort #1877，R78）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2955–T2956，impl 1478）。
> 对账口径延续十三波惯例：快照补登前置 + README 先落 + 全仓 clean verify
> + 台账四断言 + Wave 14 排程落图。

## Problem Statement

O 系已落 77 轮，需第十三次周期核账：R73–R77 新公共类型快照补登、
三门全绿、Wave 14 排程落图；期间吸收并行会话号段撞车（R73–R76 被
并行 O 会话先占，本系 R73–R75 三轮改挂 R77 空闲位重排）与共享快照
 regenerate 一并吸收 Q 系五类型（1085→1091）。

## Solution

R78 对账轮：快照补登（QuorumConsistency 入 1091 行档）+ README 先落
+ api-surface.md 同步 + 全仓 clean verify + 台账四断言 + Wave 14 落图。

## User Stories

1. 作为 O 会话驾驶者，R78 后 Wave 14 无欠账带入（撞车重排已对账）。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十三波）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿（显式退出码）。

## Out of Scope

- 不修非本系问题；不代登记 Q 系 api-surface.md 条目（各系文书自治）。

## Further Notes

- R73–R75 号段撞车是并行会话制度首次实撞：远端先落者为准、本地改挂
  空闲位重排——号段声明先行 + 每轮 fetch 复核的制度价值再次实证。
