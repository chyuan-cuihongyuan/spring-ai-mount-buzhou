# Spec 159 — 投递序列号围栏（effort #108）

> wayfinder map：`.wayfinder/maps/effort-108.md`（T517–T518）。借鉴：Kafka producer
> sequence numbers + consumer gap detection——at-least-once 之上让<b>丢包显形</b>。

## Problem Statement

webhook at-least-once 投递的接收方靠幂等键去重（spec 20），但「哪条没来」
不可见：outbox 积压/死信/静默丢弃在接收侧表现为「没有那条」——无从与
「从来没发过」区分。Kafka 的答案是单调序列号：缺号=丢，重号=重投，跳变=
生产者重启。

## Solution

**发送侧**（WebhookEventForwarder）：信封新增 `seq`——每 forwarder 进程内
单调递增（AtomicLong；重启复位 = 新纪元，fence 据此识别）。旧接收方忽略
新字段零影响。

**接收侧**（`SequenceFence`，core/webhook）：per 订阅流追踪上次序号，四裁决：

- `CONTINUE`——seq +1 连续（或首见/无 seq 旧信封——兼容放行）；
- `GAP(expected, seen)`——跳号（中间有丢失——接收方该对账拉取）；
- `DUPLICATE`——重号（at-least-once 正常现象——幂等去重既有）；
- `RESET`——seq 变小（发送方重启新纪元——重置基线继续）。

## User Stories

1. 作为接收方，GAP 裁决告诉我缺了哪些号——主动对账补拉，静默丢失变显式。
2. 作为接收方，DUPLICATE 提示走幂等去重（既有 eventId 键照用），RESET 提示
   发送方重启（纪元切换，统计基线重置）。
3. 作为运维，缺口率/重投率两个数就是投递质量面板。

## Implementation Decisions

- forwarder 一行信封注入（seq 计数器进程内——持久全局序留档）。
- fence per (subscriptionId) 独立基线；无状态锁（单订阅流内串行假设——
  webhook 投递单线程 dispatcher 天然串行，诚实边界）。

## Testing Decisions

- forwarder：连续投递信封 seq 单调递增（HTTP 收件断言）。
- fence：连续 CONTINUE / 跳号 GAP 带缺口 / 重号 DUPLICATE / 变小 RESET 后
  继续 CONTINUE / 无 seq 兼容放行。

## Out of Scope

- exactly-once；持久跨重启序号；多实例全局序。

## Further Notes

- 投递可观测三件套：outbox lag（135）→ 类型路由（151）→ 序号围栏（本轮）。
