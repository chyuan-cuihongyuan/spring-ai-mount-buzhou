# Spec 4021 — 工作窃取对半分割（effort #4021，R22）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6043–T6044，impl 2122）。
> 借鉴：Cilk THE 双端队列；Go scheduler/Ray/Dask steal-half 变体。

## Problem Statement

存量负载失衡（一 worker 堆积、其他闲置）——「新任务去哪」（两择）
之外「**存量怎么搬**」缺裁决件。

## Solution

`WorkStealingSplit`（core/concurrent，纯裁决）：

- owner 一端 LIFO 热取（缓存友好——最新最深任务先跑）；闲工从
  **另一端**冷偷（最老任务——依赖浅可独立展开）；
- 偷取量**对半**（victimSize/2，下限 minSteal、上限 victimSize；
  空 0）——一次摊平梯度，减少偷取风暴往返；
- stealCount 定量 + stealFrom 冷端搬运两 面。

## User Stories

1. 作为调度作者，闲工一次偷半——失衡梯度快速摊平。
2. 作为执行作者，热端不动（缓存局部性）、冷端出走（独立展开）。

## Testing Decisions

- 定量表（0/1/2/3/4/10/11 → 0/1/1/1/2/5/5）；五元双端队冷端偷 2
  热端不动；百项连续三偷 50/25/12 摊平；单元素与空队行为；
  畸形三型 fail-fast。

## Out of Scope

- 不做真线程/锁（并发安全归运行时）；不做随机受害者选择；
  不做偷一个的 Cilk 原版档（对半口径单一）。

## Further Notes

- 与 TwoChoiceSelector（到达时刻择短）互补：新任务去哪 vs
  存量失衡怎么搬。
- 里程碑：22/50。
