# Spec 7049 — U 系 U50 收口对账（effort #7049，U50）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7299–U7300，impl 2301）。
> 收口对账轮（U 会话 50/50 封卷）。

## Scope

- 快照补登 +1（1287→1288：KadaneMaxSubarray——U49）；
- api-surface.md 同步 + CONTEXT 计数同步（1288×13）；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7048
  零缺位）+ push 封卷；
- 封卷声明：U 会话 7000 系 50 轮收口——48 组件轮（47
  新公共类型 + U47 号段保留）+ 9 对账轮（U1/U6/U12/U18/
  U24/U30/U36/U42/U48/U50 十次门禁含收口）；勘误全量
  入档（负权哨兵冲突/口算锚值/测试竞态等——各轮 spec
  与对账轮记录）；V 会话（8000 系）另开新图。

## Out of Scope

- 跳房子哈希/IntervalHeap/流式估计族等雾区静脉保留
  （后续会话按轮 grep 认领）。

## Testing Decisions

- 快照门 diff 仅 +1 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。
