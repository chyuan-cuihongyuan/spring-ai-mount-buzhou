# Spec 7029 — U 系 U30 周期对账（effort #7029，U30）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7259–U7260，impl 2281）。
> 周期对账轮（5/50=60% 前哨——Wave 5 封波）。

## Scope

- 快照补登 +5（1268→1273：ShamirSecretSharing——crypto +
  PatienceLis/TimeBucketReservoir/Bm25Ranker/SortedRunMerge
  ——metrics）；
- api-surface.md 同步 + CONTEXT 计数同步（1273×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7028
  零缺位）。

## Out of Scope

- 不改已入档组件行为。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
