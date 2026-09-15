# Spec 1871 — O 系 R72 对账轮（effort #1871，R72）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2943–T2944，impl 1472）。
> 对账口径延续十二波惯例：快照补登前置 + README 先落 + 全仓 clean verify
> + 台账四断言。

## Problem Statement

O 系已落 71 轮，需第十二次周期核账：R67–R71 五个新公共类型快照补登、
三门全绿、Wave 13 排程落图。

## Solution

R72 对账轮：快照补登前置（五类型）+ README 先落 + 全仓 clean verify +
台账四断言 + Wave 13 落图。

## User Stories

1. 作为 O 会话驾驶者，R72 后 Wave 13 无欠账带入（逼近半程）。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十二波）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿（显式退出码）。

## Out of Scope

- 不修非本系问题。

## Further Notes

- 十二波节奏稳定；R67 哨兵假阳性与 R71 局部方法误用均为实现期自查
  修正——测试先行抓缺陷的设计持续生效。
