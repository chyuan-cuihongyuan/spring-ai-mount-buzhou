# Spec 6004 — Monotonic Deque 单调队列（effort #6004，T5）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6209–T6210，impl 2205）。
> 借鉴：单调队列滑动窗口最值思想（网络限流/行情系统同源）。

## Problem Statement

固定窗口最值的病：每窗重扫 O(k·n)（滑动放大），有序堆
O(n log k)（出窗元素需惰性墓碑删除）——**每元素摊还 O(1)
面**缺失。

## Solution

`MonotonicDeque`（core/metrics，固定窗最大值流式聚合）：

- 定容窗口：`offer` 追加值，队尾 ≤ 新值者从尾弹出（保持
  队内值严格递减——队首恒为窗最大），队首越窗从首弹出；
- 每元素至多入队/出队各一次（**摊还 O(1)**，无墓碑）；
- 读数：max（null=窗空）/size；fail-fast：capacity≤0。

## User Stories

1. 作为行情作者，百万点流的滚动最大值零对数开销。
2. 作为审计作者，同输入序列同 max 序列——确定性可回放。

## Testing Decisions

- 固定种子 500 点窗 7 全程暴力扫圣像全等；升/降/全等输入
  边界；越窗弹出语义钉住；双实例同操作同 max 序列；
  capacity≤0 fail-fast。

## Out of Scope

- 不做最小值面（对称构造同型）；不做变窗（定容定构）；
  不做并发加锁。

## Further Notes

- 与 BlockedSlidingCounter（S41）同族不同面：近似计数+误差
  界 vs 精确最值流式聚合；与 SparseTable（T4）同族不同面：
  静态离线 O(1) vs 流式在线摊还 O(1)。
- 里程碑：T5/50（10%）。
