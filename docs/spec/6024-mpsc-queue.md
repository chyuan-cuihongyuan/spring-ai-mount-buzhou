# Spec 6024 — MPSC 有界队列（effort #6024，T25）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6249–T6250，impl 2225）。
> 借鉴：JCTools MpscArrayQueue 思想。

## Problem Statement

事件汇聚的病：无界队列积压不可见（OOM 隐患），双锁队列
消费端与生产端争用（吞吐损失）——**有界 MPSC 专用面**
缺失。

## Solution

`MpscQueue`（core/concurrent）：

- 预分配环+双单调序号（producerIndex/consumerIndex）：
  生产端互斥占位写入（同监视器保可见性），消费端单线程
  无锁推进——两侧各自单调、FIFO 有界；
- 满拒新 offer false（诚实背压）；poll 空返回 null；
- 读数：size/capacity/isEmpty；fail-fast：capacity≤0、
  null 元素。

## User Stories

1. 作为汇聚作者，多源日志单线程落盘——无锁消费端。
2. 作为容量作者，满载拒新可见——背压诚实。

## Testing Decisions

- 4 生产者×2000 单消费不丢不重+单产内保序；容量上界全程
  ≤capacity；单线程 FIFO+满拒新；fail-fast。

## Out of Scope

- 不做无锁生产端（两阶段 claim 展望——并发正确性优先）；
  不做 MPMC。

## Further Notes

- 与 DisruptorRingBuffer（5008）同族不同面：多生产者 FIFO
  vs 单生产者事件派环；与 BoundedMailbox（5020）不同面：
  并发生产端 vs 策略性溢出。
- 里程碑：T25/50（50%）。
