# Spec 2014 — 键压缩日志语义（effort #2014，R15）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3129–T3130，impl 1565）。
> 借鉴：Kafka log compaction——同 key 保留最新 + tombstone 墓碑删除。

## Problem Statement

状态类日志（工具调用台账 / 会话特征 / 会话迁移对账记录）按键累积多版
本：读取终态要全量扫 + 逐 key 挑最新——读放大随历史线性涨；删除语义
缺位（「这个 key 作废了」只能隐式——读取方各自猜）。

## Solution

`KeyCompaction`（core/recovery，纯函数零状态）：

- `Entry(key, seq, payload)`——payload 为 null 即 tombstone 墓碑；
- `compact(entries)`：同 key 取 seq 最大者；胜者是墓碑 → key 删除；
  同 seq 取后见者（严格递增序号下的退化口径）；**乱序幂等**——保留
  者只由 (key, maxSeq) 决定，与输入顺序无关（Kafka 压缩语义核心）；
- 墓碑后更高 seq 复活、旧墓碑不遮新值；
- `CompactionResult`：终态键值 + tombstones/superseded 计数 +
  compactionRatio（压缩率）；
- 契约：key 非空、seq ≥ 0 fail-fast；null 集合=空。

## User Stories

1. 作为台账读者，一遍压缩即终态——不用全量扫逐 key 挑最新。
2. 作为删除作者，写一条墓碑即显式作废——不再「各自猜」。
3. 作为容量观测者，compactionRatio 高=历史冗余多——归档/清理有据。

## Testing Decisions

- 最新 seq 胜；乱序幂等（正反序同结果）；墓碑删除；墓碑后复活；旧
  墓碑不遮新值；同 seq 后见者；压缩率与空不除零；null 集合空；畸形
  两型 fail-fast。

## Out of Scope

- 不做增量压缩（全量入口函数口径）；不接具体 store（读取桥接归
  后续轮）。

## Further Notes

- 与事件溯源回放（core/recovery 族）互补：回放保因果全序，压缩取
  键终态。
