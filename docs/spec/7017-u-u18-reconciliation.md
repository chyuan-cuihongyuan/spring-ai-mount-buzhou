# Spec 7017 — U 系 U18 周期对账（effort #7017，U18）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7235–U7236，impl 2269）。
> 周期对账轮（3/50=36% 前哨——Wave 3 封波）。

## Scope

- 快照补登 +5（1258→1263：ConvexHull/ClosestPair/
  PointInPolygon/BresenhamLine——policy + Manacher——metrics）；
- api-surface.md 同步 + CONTEXT 计数同步（1263×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7016
  零缺位）。

## Out of Scope

- 不改已入档组件行为；不新增非对账公共类型。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
