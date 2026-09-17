# Spec 3030 — 时间轮定时器（effort #3030，R31）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5061–T5062，impl 2031）。
> 借鉴：hashed timing wheel（Netty HashedWheelTimer / Kafka pctimer）。

## Problem Statement

大量定时任务（超时/重试/租约续期/排空窗）用优先队列逐次比较
O(log n) 入队出队、且到期扫描全量有序维护贵——海量短定时任务的
经典解是时间轮：入 O(1)、到期只扫掠过槽。

## Solution

`HashedWheelTimers`（core/concurrent，免线程纯数据结构件——推进
归调用方 tick 循环）：

- `schedule(now, delay, label)` O(1) 入轮：deadline 落槽
  (deadline/tick)%size，大延迟**多轮滞槽**（时针掠过校验，未到
  留槽）；返回单调 id；
- `advanceTo(now)` 时针逐 tick 前进，掠槽弹出 deadline ≤ now 者
  （返回序：deadline 先、同刻 id 序——确定性）；
- 时间调用方传入（确定性免注入时钟）；守恒对账 scheduled ==
  fired + pending；粒度诚实边界：到期校验在**槽重访时**——deadline
  过后最多再等一个轮周长（wheelSize×tick，最大额外延迟显式可诺）；
  槽 ≥2 / tick ≥1 / delay ≥0 校验；不做取消（留白）。

## User Stories

1. 作为调度作者，万级短定时任务 O(1) 入轮——免优先队列全序维护。
2. 作为对账作者，守恒恒等与确定性 firing 序可断可回放。

## Testing Decisions

- 到期边界（100 未到空/150 恰到出/再进不出）；多轮滞槽（轮长
  80ms 延迟 300——240 不出 300 出）；槽回绕；同刻 id 序+跨刻
  deadline 序；2000 随机操作守恒逐次断言+终局清空；粒度边界
  （轮同步后 delay 0——槽已掠过，轮周长内必出）；参数三路
  fail-fast。

## Out of Scope

- 不做取消/续期 handle（生命周期管理留白）；不做多级轮（层级
  时间轮留白）；不做线程驱动（tick 循环归调用方）。

## Further Notes

- 与 EdfScheduler（截止期序精确弹出）互补：EDF 是**取最优**队列、
  时间轮是**批量到期**结构——按规模与精度选型。
- 里程碑：31/150。
