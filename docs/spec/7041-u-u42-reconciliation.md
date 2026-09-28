# Spec 7041 — U 系 U42 周期对账（effort #7041，U42）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7283–U7284，impl 2293）。
> 周期对账轮（7/50=84% 前哨——Wave 7 封波）。

## Scope

- 快照补登 +5（1278→1283：SnowflakeIdGenerator——concurrent
  + FastModPow——crypto + ChiSquareUniformity/GrayCodeSequence/
  JosephusPermutation——metrics）；
- api-surface.md 同步 + CONTEXT 计数同步（1283×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7040
  零缺位）。

## Out of Scope

- 不改已入档组件行为。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
