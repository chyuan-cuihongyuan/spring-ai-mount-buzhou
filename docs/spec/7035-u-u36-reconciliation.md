# Spec 7035 — U 系 U36 周期对账（effort #7035，U36）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7271–U7272，impl 2287）。
> 周期对账轮（6/50=72% 前哨——Wave 6 封波）。

## Scope

- 快照补登 +5（1273→1278：XxHash64——message +
  EulerianPath/QuickSelect——concurrent +
  KahanSummator/ZobristHashing——metrics）；
- api-surface.md 同步 + CONTEXT 计数同步（1278×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7034
  零缺位）。

## Out of Scope

- 不改已入档组件行为。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
