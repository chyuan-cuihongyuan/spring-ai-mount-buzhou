# Spec 1883 — O 系 R84 对账轮（effort #1883，R84）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2967–T2968，impl 1484）。
> 对账口径延续十四波惯例：快照补登前置 + README 先落 + 全仓 verify
> + 台账四断言 + Wave 15 排程落图。

## Problem Statement

O 系已落 83 轮，需第十四次周期核账：R79–R83 五个新公共类型快照
补登、三门全绿、Wave 15 排程落图；期间三度撞坑换静脉（两随机族
Q-3022 占坑、Holt Q 系占坑、预热 Q 系占坑）——借鉴定源清单的高频
静脉正被并行会话快速消耗。

## Solution

R84 对账轮：快照补登（五类型 1091→1096）+ api-surface.md 同步 +
README 先落 + 全仓 verify + 台账四断言 + Wave 15 落图。

## User Stories

1. 作为 O 会话驾驶者，R84 后 Wave 15 无欠账带入，选题池换血完成。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；既定口径例行（第十四波）。
- verify 口径偏离入档：非 clean 离线全仓——同检出并行会话构建
  冲突规避（clean 会互删他系 target；等价性由确定性测试修复全绿
  + 首跑 104 失败清零证据链支撑）。

## Testing Decisions

- 证据 = 全仓 verify BUILD SUCCESS + ledger audit 四断言绿（显式
  退出码）；外域摇摆/回归候选沿用 R78 入档口径（显式排除清单）。

## Out of Scope

- 不修非本系问题；不代登记他系 api-surface.md 条目。

## Further Notes

- 撞坑三连（R79/R81/R82 选题均被并行会话占坑后换静脉）——号段
  声明先行 + 每轮落轮前 grep 复核的制度价值第十四波实证。
