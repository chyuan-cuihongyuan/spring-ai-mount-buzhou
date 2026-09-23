# Spec 5020 — Bounded Mailbox 有界信箱（effort #5020，S21）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6141–T6142，impl 2171）。
> 借鉴：Akka bounded mailbox（容量信箱 + 溢出策略显式声明）。

## Problem Statement

消息接纳的病：无界队列（积压不可见——OOM 隐患）或静默
丢弃（丢了什么不知道）——**容量信箱 + 显式溢出策略面**
缺失。

## Solution

`BoundedMailbox`（core/backpressure）：

- `offer(item)`：未满入队；满则按策略——`DROP_NEWEST` 拒新
 （返回 false）、`DROP_OLDEST` 逐最旧纳新（droppedCount++
  且返回 true——新消息不因积压被拒）；
- `poll()`：FIFO 头部取（空返回 null）；
- 读数：size/droppedCount/capacity——积压与丢弃可见；
- fail-fast：capacity≤0、null policy/item。

## User Stories

1. 作为消息处理作者，积压上限显式、溢出代价显式可观测。
2. 作为审计作者，同投递序列同信箱状态（确定性可回放）。

## Testing Decisions

- 两种策略分叉（满时拒新 vs 逐旧纳新 + dropped 计数）；
  FIFO 次序；空取 null；capacity≤0/null fail-fast；确定性
  回放。

## Out of Scope

- 不做阻塞等待语义（非阻塞口径）；不做优先级信箱；不做
  持久化信箱。

## Further Notes

- 与 DisruptorRingBuffer（spec 5008）同族不同面：预分配环
  零分配 vs 策略性溢出信箱。Wave 4 第三件。
- 里程碑：S21/50（42%）。
