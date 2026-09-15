# Spec 1840 — 向量时钟偏序比较（effort #1840，R41）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2881–T2882，impl 1441）。借鉴：
> Dynamo 向量时钟 / Lamport happens-before——无中心时钟下判因果序，
> 并发（CONCURRENT）判定是冲突解的前提。

## Problem Statement

多写方（多实例会话/并行工具）的事件只带墙钟时间戳：时钟偏移下「谁先」
不可判，真并发（无因果关系的冲突写）与因果先后无法区分——冲突解策略
无从谈起。

## Solution

`VectorClockOrder`（core/concurrent，静态纯函数）：

- `compare(a, b)` → 三态 `BEFORE / AFTER / CONCURRENT`：键域并集逐分量
  比（缺席按 0——稀疏表示合法）；一方各分量 ≤ 且至少一严格小即因果序；
  互相各有领先分量即并发；
- 相等/双空按 BEFORE 退化（「不后于」语义）；负分量 fail-fast。

## User Stories

1. 作为冲突解作者，CONCURRENT 才进冲突解队列（真冲突）；BEFORE 直接
   取新弃旧（有因果序不是冲突）。
2. 作为多写方协调者，向量时钟自维护（写前递增本方分量），比较零中心
   依赖。
3. 作为审计者，因果序判定确定性可回放（无墙钟偏移敏感）。

## Implementation Decisions

- 纯判序不合并（时钟 merge 归宿主）；缺席=0 语义入档（稀疏时钟合法）。

## Testing Decisions

- 因果先/后；并发互领；稀疏缺席按 0；相等与 null 退化 BEFORE；负分量
  fail-fast。

## Out of Scope

- 不实现时钟维护/合并；不接具体存储（会话/事实层接线归后续轮）。

## Further Notes

- 与 SequenceFence 正交：那是单调序号护栏，这是因果偏序比较。
