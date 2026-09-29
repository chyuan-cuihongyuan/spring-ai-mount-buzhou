# Spec 8023 — V 系 V24 周期对账（effort #8023，V24）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8047–V8048，impl 2325）。
> 对账轮（Wave 4 收口——24/50=48%）。

## Scope

- 快照批补登 +5（1302→1307：BinomialHeap——concurrent +
  LfuEviction——cache + RadixSorter——metrics + IntervalHeap/
  LeftistHeap——concurrent（V19/V20 先行提交件补入批——快照门
  真值逮住漏登——三门防线的价值实证））；
- api-surface.md 同步 +3 行 + CONTEXT 计数同步（1307×13）；
- 全仓 16 模块 verify 三门绿（R48 协议口径）+ V 系第四波
  对账门（spec 8000–8022 零缺位）+ 对账轮尝试 push；
- 勘误入档：FibonacciHeap 复杂度超时预算退雾区（同族二项
  堆补位——V21）；QuotientFilter 删除簇边界退雾区（滑动窗
  口最小覆盖补位——V15）。

## Out of Scope

- Wave 5（采样决策族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +3 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V24/50（48%）。
