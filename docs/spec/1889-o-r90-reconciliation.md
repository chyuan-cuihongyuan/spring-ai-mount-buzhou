# Spec 1889 — O 系 R90 对账轮（effort #1889，R90）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2979–T2980，impl 1490）。
> 对账口径延续十五波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 16 排程落图。

## Problem Statement

O 系已落 89 轮，需第十五次周期核账：R85–R89 五个新公共类型快照
补登、三门全绿、Wave 16 排程落图（半程 90/150 达成）。

## Solution

R90 对账轮：快照补登（五类型 1096→1101）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 16 落图。

## User Stories

1. 作为 O 会话驾驶者，R90 后 Wave 16 无欠账带入（半程里程碑）。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十五波）；verify 口径沿用
  非 clean 离线（同检出并行会话冲突规避，R84 已入档）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿（显式
  退出码）；外域摇摆/回归候选沿用 R78 入档口径。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- 90/150 = 60% 里程碑；撞坑监察常态化（每轮落轮前 grep）下 Wave 15
  五轮全部首选题落位，零换静脉。
