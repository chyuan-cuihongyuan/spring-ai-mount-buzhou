# Spec 7023 — U 系 U24 周期对账（effort #7023，U24）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7247–U7248，impl 2275）。
> 周期对账轮（4/50=48% 前哨——Wave 4 封波）。

## Scope

- 快照补登 +5（1263→1268：Soundex/InvertedIndex/
  DamerauLevenshtein——metrics + GolombRiceCodec——message +
  WriteAheadLog——fs）；
- api-surface.md 同步 + CONTEXT 计数同步（1268×13）；
- Wave 4 README/api-surface 文档行补登（补账批前置入档）；
- 勘误入档：U23 原拟跳房子哈希（实现复杂度超时预算）按
  「不可自洽即换静脉」纪律退回雾区——DamerauLevenshtein
  补位（见 spec 7022）；Wave 5 原拟 IntervalHeap 同款退回
  ——PatienceLis 补位；
- 全仓 16 模块 verify 三门绿 + 台账核账（spec 7000–7022
  零缺位）。

## Out of Scope

- 不改已入档组件行为。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿。
