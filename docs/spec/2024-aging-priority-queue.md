# Spec 2024 — 老化优先级队列（effort #2024，R25）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3149–T3150，impl 1575）。
> 借鉴：OS 调度 aging——等待生息反饥饿。

## Problem Statement

静态优先级的已知病是饥饿：高优先级洪峰下低优先级任务永不出队
（SpawnPriority 定序无时间维）。反饥饿需要「等待本身升值」。

## Solution

`AgingPriorityQueue<T>`（core/exec，synchronized 小临界区）：

- `enqueue(item, basePriority ∈ [0,100], now)`；
- 有效优先级 = base + 等待秒 × agingRate（默认 1 点/秒——百级差百秒
  内反超）；`poll(now)` 取有效最大，**同分入队序先出（FIFO 保序）**；
- 零速率退化静态优先级（显式口径）；`snapshotByEffectivePriority(now)`
  观测面（不出队）；size 水位；
- poll 线性扫描 O(n)（准入级小规模——诚实边界入档）；时间外注入
  确定性可回放。

## User Stories

1. 作为调度作者，低优先级任务等待够久必反超新来的高优先级——饥饿
   有时间下界，不再无限。
2. 作为观测者，快照面看有效优先级序——谁在涨、谁将被反超可预读。

## Testing Decisions

- 静态序（零速率）；90 秒老住户（10）反超新来者（90）；差距未够新
  来者仍胜；同分 FIFO；零速率退化；空队 null；快照不动队列；畸形
  六型 fail-fast。

## Out of Scope

- 不做堆优化（O(n) 口径）；不接 SpawnGate 装配（接线归后续轮）。

## Further Notes

- 与 SpawnPriority（静态定序）正交互补：静态序 + 时间升值 = 无饥饿
  的优先级调度。
