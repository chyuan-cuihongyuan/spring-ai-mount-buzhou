# Spec 7011 — U 系 U12 周期对账（effort #7011，U12）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7223–U7224，impl 2263）。
> 周期对账轮（2/50=24% 前哨——Wave 2 封波）。

## Scope

- 快照补登 +5（1253→1258：BellmanFord/FloydWarshall/
  KruskalMst/ArticulationPoints/LcaLifting——concurrent）；
- api-surface.md 同步 + CONTEXT 计数同步（1258×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7010
  零缺位）；
- 避让记录入档：Wave 2 原拟流式估计族（HLL/Morris/
  水位窗）经全量包盘点确认被 A–T 系占坑
  （HllCardinalitySketch/MorrisCounter/OutOfOrderWindow），
  按纪律换静脉——图算法族提前，流式估计移回雾区待
  后续轮按轮 grep。

## Out of Scope

- 不改已入档组件行为；不新增非对账公共类型。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
