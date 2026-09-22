# Spec 1901 — O 系 R102 对账轮（effort #1901，R102）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3003–T3004，impl 1502）。
> 对账口径延续十七波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 18 排程落图。

## Problem Statement

O 系已落 101 轮，需第十七次周期核账：R97–R101 五个新公共类型快照
补登、三门全绿、Wave 18 排程落图。

## Solution

R102 对账轮：快照补登（五类型 1106→1111）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 18 落图。

## User Stories

1. 作为 O 会话驾驶者，R102 后 Wave 18 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十七波）；verify 沿用非 clean
  离线口径（R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿；外域
  摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- Wave 17 撞坑两连（HPA 稳定窗→923 占坑、告警抑制→330 占坑）均
  换静脉成功——每轮落轮前 grep 制度持续生效。
