# Spec 7005 — U 系 U6 周期对账（effort #7005，U6）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7211–U7212，impl 2257）。
> 周期对账轮（1/50=12%前哨——Wave 1 封波）。

## Scope

- 快照补登 +4（1249→1253：ZArray/SqrtDecomposition——metrics +
  AvlTree——concurrent + RunLengthCodec——message）；
- api-surface.md 同步 + CONTEXT 计数同步（1253×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7004 零
  缺位）。

## Out of Scope

- 不改已入档组件行为；不新增非对账公共类型。

## Testing Decisions

- 快照门 diff 仅 +4 逐行核对；对账门/覆盖门全绿。
