# Spec 5008 — Disruptor 环形缓冲（effort #5008，S9）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6117–T6118，impl 2159）。
> 借鉴：LMAX Disruptor（预分配槽 + 序标两段式 claim/publish）。

## Problem Statement

高频事件排队的病：有界队列每次入队/出队分配节点（GC 压力）
与锁竞争（CAS 队尾单点）——**预分配 + 序标索引面**缺失。

## Solution

`DisruptorRingBuffer`（core/backpressure）：

- 预分配槽数组（容量须 2 的幂——`sequence & mask` 免模导航，
  否则 IAE）；
- 两段式：`claim()` 认领序标（claimCursor 推进）→ `publish`
  填槽并推进 publishCursor（只推到最大连续已发布）→
  `tryConsume(seq)`（未发布返回 null——非阻塞）；
- 槽复用零分配（wrap 覆盖由消费侧 gating 保证——单消费者
  口径诚实入档，gating 多消费者留后）；
- 读数：claimCursor/publishCursor/capacity；
- fail-fast：非 2 幂容量、publish 越权（他人序号）、null item。

## User Stories

1. 作为事件管线作者，入队/出队零分配、两段式解耦认领与发布。
2. 作为审计作者，同操作序列同槽轨迹（确定性可回放）。

## Testing Decisions

- 两段式时序（claim 未 publish 不可消费）；顺序消费与跨
  wrap 数据完整性（容量 4 灌 10 条）；publishCursor 只连续
  推进（乱序 publish 被拒）；非 2 幂/越权/null fail-fast。

## Out of Scope

- 不做多生产者 gating（单生产者口径）；不做等待策略族
 （busy/yield/blocking）；不做事件批消费。

## Further Notes

- 与阻塞背压管线（spec 39）互补：有界阻塞队列 vs 预分配
  无锁环。Wave 2 第四件。
- 里程碑：S9/50（18%）。
