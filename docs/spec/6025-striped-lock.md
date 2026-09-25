# Spec 6025 — Striped Lock 条带锁（effort #6025，T26）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6251–T6252，impl 2226）。
> 借鉴：Guava Striped 思想。源码于 T24 核账批预入档。

## Problem Statement

键级互斥的病：每键一把锁（内存随键基数爆炸）与全局单锁
（无关键互相阻塞）——**固定条带降争用面**缺失。

## Solution

`StripedLock`（core/concurrent，源码已预载）：

- n 把 ReentrantLock（向上取 2 的幂）覆盖无限键空间：
  SplitMix64 混淆 + 位掩码取模——相近键散开、热键恒同锁；
- withLock(key, action) 两条重载；stripeCount/indexFor
  审计读数；fail-fast：stripes≤0。

## User Stories

1. 作为缓存作者，按 key 互斥而不互相拖累——降争用底座。
2. 作为审计作者，同键恒同条带——映射可复算。

## Testing Decisions

- 同键恒同锁实例；4 线程×5000 同键互斥计数精确；不同键
  可并行（临界区重叠观测）；条带 2 的幂取整；fail-fast。

## Out of Scope

- 不做强公平条带（ReentrantLock 默认）；不做键级精确锁。

## Further Notes

- 与 TicketLock（5006）同族不同面：单锁 FIFO 公平 vs 键
  空间分条降争用。
- 里程碑：T26/50（52%）。
