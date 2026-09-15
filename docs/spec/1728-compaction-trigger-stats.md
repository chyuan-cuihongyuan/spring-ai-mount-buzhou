# Spec 1728 — 压缩触发原因分布（effort #1728，R29）（effort #1728，R29）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2657–T2658，impl 1328，impl RocksDB / Cassandra compaction stats）。借鉴：压缩被什么触发（空闲闲时/比例越线/人工/检查点前）无分布——触发阈值调参方向不明，闲时压缩空转还是越线救火不可见。

## Problem Statement

`CompactionTriggerStats`（memory/compact，实例面线程安全）：Trigger 四闭集（IDLE/RATIO/MANUAL/CHECKPOINT）+record+census+idleShare（无样本 −1）+resetForTest。与 CompactionRatioStats（压缩率面）互补。纯读面 opt-in。

## Solution

作为调参者，IDLE 占比高 → 阈值合理（闲时干活）。

## User Stories

1. 17280
2. 17281
3. 17282

## Implementation Decisions

- 17283

## Testing Decisions

- 17284

## Out of Scope

- 17285

## Further Notes

- 17286
