# Spec 3003 — 最早截止期优先队列（effort #3003，R4）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5007–T5008，impl 2004）。
> 借鉴：EDF（Earliest Deadline First，实时调度经典——单处理器可调度最优）。

## Problem Statement

轮次 deadline / 工具超时 / 优雅排空窗等多源时限并发在队时，「谁
最先到期」若靠各处自扫比较或遍历取最小，口径分散且同刻并列时顺序
抖动——缺一个统一、确定性的截止期序原语。

## Solution

`EdfScheduler`（core/concurrent，单消费者口径）：

- `offer(deadline, id)` 入队（截止期可负——已过期照常排队）；
- `poll()/peek()` 截止期升序，**同截止期按入队序 FIFO**（tie-break
  确定性）；空返回 null（JDK Queue 同约定）；
- `nextDeadline()` 空队列 +∞（NO_DEADLINE 口径——比较语义恒安全）；
- `headLaxity(now)` 队首余量 = 截止期 − now（负即已错过——超期可判）；
- `Pending(deadline, sequence, id)` 记录承载全息（序号单调入队号）。

## User Stories

1. 作为调度作者，多源时限单队列统一取首——免各处自扫最早。
2. 作为告警作者，headLaxity 告负即判超期——余量可读可断。

## Testing Decisions

- 截止期乱序入队出队序正确；同刻三任务 FIFO；空态四读一致
  （null/null/+∞/0）；peek 非破坏；laxity 正/零/负三段；过期
  （负截止期）先出；Pending 记录全息与序号单调；size/isEmpty
  生命周期守恒。

## Out of Scope

- 不做抢占/迁移（纯队列口径）；不做多消费者并发（接线归后续轮）；
  不做定时器线程（触发归调用方时钟）。

## Further Notes

- 与 ScheduleFloat（CPM 松弛量）/ Turn deadline 互补：本件是
  **排队原语**，松弛计算与截止期裁定归既有件。
- 里程碑：4/150。
