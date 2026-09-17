# Spec 3016 — 批量攒批器（effort #3016，R17）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5033–T5034，impl 2017）。
> 借鉴：Kafka producer 攒批（batch.size + linger.ms 双阈值）。

## Problem Statement

事件外发 webhook / 指标上报 / 批量落盘若逐条即时处理，单条开销
摊不平；只按条数攒又把延迟不可控地拉大——吞吐与延迟需要**显式
双旋钮**权衡。

## Solution

`BatchAccumulator<T>`（core/concurrent，单攒批线程口径）：

- `offer(item, now)` 入批（首入时间为批龄锚），条数达 max 返回
  true（应立即冲）；
- `flushReady(now)`：**条数满 或 批龄 ≥ linger** 任一成立；
- `drain()` 取走整批（保序；空批空表不动账）——批龄重锚；
- 时间由调用方传入（确定性免注入）；守恒对账面：
  totalOffered == totalFlushed + 在批 size；双阈值 ≥1 校验。

## User Stories

1. 作为上报作者，攒满大批摊薄开销、稀流龄到即冲——延迟上限
   linger 显式可诺。
2. 作为对账作者，守恒恒等式可断——溢出零丢失。

## Testing Decisions

- 条数满龄 1ms 即冲；龄恰 100 达界即冲（99 不冲）；双未达持有；
  drain 重锚批龄（新批龄从新首入计）；2000 随机操作守恒恒等逐次
  断言；空批 drain 不动账；三批循环账面（flushCount 3 /
  totalFlushed 5）；配置 0/负 fail-fast；冲出保序。

## Out of Scope

- 不做定时器线程/调度（触发归调用方——本件是纯判定与持有）；
  不做键分桶多批（每桶一件）；不做并发冲批。

## Further Notes

- 与并发组闸/EDF 互补：闸限并发形状、EDF 排时限序，本件管
  **批量时机**。
- 里程碑：17/150。
