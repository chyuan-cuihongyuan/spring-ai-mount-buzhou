# Spec 1865 — O 系 R66 对账轮（effort #1865，R66）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2931–T2932，impl 1466）。
> 对账口径延续十一波惯例：快照补登前置 + README 先落 + 全仓 clean verify
> + 台账四断言。

## Problem Statement

O 系已落 65 轮，需第十一次周期核账：R61–R65 五个新公共类型快照补登、
三门全绿、Wave 12 排程落图。

## Solution

R66 对账轮：快照补登前置（五类型）+ README 先落 + 全仓 clean verify +
台账四断言 + Wave 12 落图。

## User Stories

1. 作为 O 会话驾驶者，R66 后 Wave 12 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十一波）；R63 号漂移修正
 （1464→1463）与 echo 掩码流程缺陷修正已入档。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿（显式退出码）。

## Out of Scope

- 不修非本系问题。

## Further Notes

- 十一波节奏稳定；号段公式漂移首次实证即被对账门当场拦截（设计生效
  的第二次活证——第一次 R18 README 漏行）。
