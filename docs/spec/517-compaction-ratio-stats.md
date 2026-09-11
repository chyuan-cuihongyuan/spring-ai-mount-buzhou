# Spec 517 — 记忆压缩率分布观测（effort #517）

> wayfinder map：`.wayfinder/maps/effort-517.md`（T785–T786）。E 会话第 18 轮。

## Problem Statement

压缩事件（34）有 CompactionListener 缝——回收字符/逐出比/折入 trigger
数据都在事件流里，但无读数面：压缩实际回收分布、逐出加压到哪级、摘要
折叠由什么触发，要靠翻事件流拼凑。

## Solution

`memory.CompactionRatioStats`（416 分位族同法；Holder 同型静态实例）：

- 微压缩：reclaimedChars 样本窗（512）→ p50/p95 + totalReclaimed；
  evictRatio 直方图（梯子三级预注册、未知值诚实新键）。
- 摘要折入：独立样本窗（128）折入字符 p50 + totalFolds + trigger 计数。
- 接线：MemoryModule 既有匿名 CompactionListener 内双写（观测零干预）；
  `MemoryModule.compactionStats()` 静态读数面。

## User Stories

1. 作为记忆调参方，我想看回收字符分布与逐出比直方图， so 梯子参数
   （0.8/0.9/1.0）是否合理有数据依据。
2. 作为运维，我想看折入 trigger 计数， so budget/backlog/drift 三信号
   谁在驱动压缩一屏可见。

## Implementation Decisions

- 折入样本独立窗（与回收字符分布分开——两列语义不互染）。
- 只观测不干预；重启清零（进程内观察面口径）。

## Testing Decisions

- 1..100 样本分位数学；梯子直方图计数；折入独立窗不互染；窗口有界；
  零样本 null；未知逐出比新键。

## Out of Scope

- 压缩策略干预；跨进程聚合；per-session 分布。

## Further Notes

- 新公共类型 `CompactionRatioStats`（嵌套 `Snapshot`）随轮 regenerate
  快照 + api-surface.md 加行。
