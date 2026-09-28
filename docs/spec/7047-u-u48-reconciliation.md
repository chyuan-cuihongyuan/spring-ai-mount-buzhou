# Spec 7047 — U 系 U48 周期对账（effort #7047，U48）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7295–U7296，impl 2299）。
> 周期对账轮（8/50=96% 前哨——Wave 8 封波）。

## Scope

- 快照补登 +4（1283→1287：TernarySearch/FloydCycleDetector
  ——metrics + LargestRectangleHistogram——concurrent +
  ActivitySelectionGreedy——policy；U47 号段保留零类型）；
- api-surface.md 同步 + CONTEXT 计数同步（1287×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7046
  零缺位）。

## Out of Scope

- 不改已入档组件行为。

## Testing Decisions

- 快照门 diff 仅 +4 逐行核对；对账门/覆盖门全绿。
