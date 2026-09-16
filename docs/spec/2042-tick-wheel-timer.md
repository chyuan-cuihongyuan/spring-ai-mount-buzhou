# Spec 2042 — 刻度轮定时器（effort #2042，R43）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3185–T3186，impl 1593）。
> 借鉴：Netty hashed wheel timer——O(1) 调度轮，纯逻辑 tick 驱动。

## Problem Statement

海量定时任务（延迟投递 / 重试排程 / 心跳检查）用逐任务堆/树是
O(log n) 每操作；时间轮把调度降到 O(1)——任务按延迟散进轮槽，tick
推进只查当前槽。工程线程版复杂难测，纯逻辑 tick 版（调用方驱动
advance）确定性可回放。

## Solution

`TickWheelTimer`（core/exec，synchronized 小临界区）：

- `schedule(taskId, delayTicks ≥ 1)`：slot = (cursor+delay) mod W（W
  为 2 的幂，默认 64），**rounds = (delay−1)/W**（跨轮圈数——首轮
  访问即到期者 0）；重复 id 幂等替换；
- `advance()`：cursor 前进一槽，当前槽内圈数尽者到期返回；未尽者
  圈数 −1 留槽（**无条件写回**——due 空也写，圈数递减不丢）；
- `cancel(id)`；读数 pendingCount / ticks；
- 契约：W ≥ 8 且 2 的幂、taskId 非空、delay ≥ 1 fail-fast。

## User Stories

1. 作为调度作者，万级延迟任务 O(1) 摊销——tick 只查一槽。
2. 作为测试作者，advance 驱动时间——确定性回放无时钟依赖。

## Testing Decisions

- 轮内恰第 5 tick 到期；跨轮（W=8 delay=20）恰第 20 tick；同槽三任
  务齐发；游标回绕后仍准（15 tick 后 delay2）；幂等重调度旧失效；
  取消不发；畸形六型 fail-fast。

## Out of Scope

- 不做真实线程/时间驱动（调用方 tick）；不做分级轮（层级时间轮留白）。

## Further Notes

- 实现教训入档：圈数公式 (delay−1)/W（非 (cursor+delay)/W——首访即
  到期多算一圈）；due 空提前 return 丢圈数递减写回。
