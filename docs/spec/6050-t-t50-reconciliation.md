# Spec 6050 — T 系 T50 收口对账（effort #6050，T50）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6301–T6302，impl 2251）。
> 收口对账轮（T 会话 50/50 封卷）。

## Scope

- 快照补登 +1（1248→1249：DijkstraShortestPath——T49）；
- api-surface.md 同步 + CONTEXT 计数同步（1249×13）；
- 勘误入档：对账门系列上界 6049→6050（T37 撞号平移后
  effort=6000+T，T50 落 6050——门随收口扩包）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 6000–6049 零
  缺位）+ 推送封卷。

## Out of Scope

- U 会话（7000 系）另开新图，不在本卷。

## Testing Decisions

- 快照门 diff 仅 +1 逐行核对；对账门含 6050 全绿；
  覆盖门 README↔docs/spec 双向无死角。
