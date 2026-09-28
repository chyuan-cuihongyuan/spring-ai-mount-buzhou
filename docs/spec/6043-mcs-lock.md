# Spec 6043 — MCS Lock 队列锁（effort #6043，T43）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6287–T6286，impl 2243）。
> 借鉴：Linux 内核 MCS 锁思想。源码于 T42 对账批预入档。

## Problem Statement

高争用互斥的病：全局自旋变量让所有等待者挤同一缓存行
（CAS 风暴）——**每等待者本地自旋面**缺失。

## Solution

`McsLock`（core/concurrent，源码已预载）：

- 等待者入队后自旋在自己的节点 locked 位上；前驱解锁时
  仅向自己的后继节点写一次（交接）；队列 FIFO 公平；
- tail CAS 入队/摘尾；Node 公开句柄（lock 返回/unlock
  消费）；fail-fast：null 句柄。

## User Stories

1. 作为内核作者，高争用临界区缓存行友好——自旋底座。
2. 作为审计作者，FIFO 获取序可观测——公平可证。

## Testing Decisions

- 4 线程×2000 临界区计数精确（互斥）；先到先入（两线程
  交接时序观测）；解锁后再锁；null 句柄 fail-fast。

## Out of Scope

- 不做 tryLock/超时；不做NUMA 感知分簇。

## Further Notes

- 与 TicketLock（5006）同族不同面：全局票号自旋 vs 每节点
  本地自旋；与 SeqLock（5007）不同面：互斥写锁 vs 乐观读
  序号。
- 里程碑：T43/50（86%）。
