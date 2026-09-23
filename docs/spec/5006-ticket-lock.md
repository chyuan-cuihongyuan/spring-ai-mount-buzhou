# Spec 5006 — Ticket Lock 票据锁（effort #5006，S7）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6113–T6114，impl 2157）。
> 借鉴：Linux 内核 ticket spinlock（取票 FIFO 公平排队）。

## Problem Statement

互斥等待的病：裸 CAS 自旋（先到未必先得——饥饿无界）或
synchronized（无排队可见性、无公平读数）——**FIFO 公平
互斥面**缺失。

## Solution

`TicketLock`（core/concurrent）：

- 取票：`lock()` 原子取号（nextTicket++）并自旋等待
  nowServing 追上自己的票——**先到先服务**（FIFO——饥饿
  结构性排除）；
- 放行：`unlock(ticket)` 校验票号即 nowServing（乱序放行
  IAE fail-fast）后推进；
- 读数面：nowServing/nextTicket/queueLength（等待深度可见）；
- 自旋对虚拟线程友好（Thread.onSpinWait + 定期 yield）；
- fail-fast：null 语义不适用（纯算术面），乱序 unlock IAE。

## User Stories

1. 作为临界区作者，等待者按到达序进入——无饥饿。
2. 作为运维作者，queueLength 可观测排队深度。

## Testing Decisions

- 票据算术（取号递增、放行推进）；乱序/越票 unlock IAE；
  并发互斥（N 线程 × 迭代计数器最终精确）+ 全员获释 +
  queueLength 归零；确定性算术回放。

## Out of Scope

- 不做读/写分票（读写锁族）；不做自适应自旋参数；不做跨
  进程语义。

## Further Notes

- 与 TicketLock 同族不同面：synchronized 无排队读数。Wave 2
  第二件（执行并发族前提）。
- 里程碑：S7/50（14%）。
