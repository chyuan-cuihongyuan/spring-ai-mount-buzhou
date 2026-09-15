# Spec 1864 — 可见性超时账（effort #1864，R65）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2929–T2930，impl 1465）。借鉴：
> AWS SQS visibility timeout——取走即隐藏防重复消费、超时未确认重回队列
> （消费方死消息不丢）、重投穷尽进死信（毒消息不死循环）。

## Problem Statement`

出站投递（webhook/事件转发）的「至少一次」语义缺时限账：投出后多久算
 失败该重投？重投几次放弃进死信？无时限则消费方死了消息永挂、无上限
 则毒消息无限循环——「不丢」与「不死循环」两难。

## Solution

`VisibilityTimeoutAccounting`（core/webhook，静态纯函数）：

- `shouldRedeliver(deliveredAt, visibilityTimeout, now, acknowledged)`：
  未确认且到点（边界含上——到点即回队）；
- `shouldDeadLetter(redeliveryCount, maxRedeliveries)`：重投计数 ≥ 上限
  （含上；零容忍合法——首败即死信）；
- `census(now, timeout, max, facts)` 三段普查：在飞/超时重投/死信候选
  分账 + 最老在飞龄 + inFlightRatio（哨兵 -1）。

## User Stories

1. 作为投递作者，超时 300ms 内未确认 → 重投；重投 3 次仍败 → 死信——
   「至少一次」有时限有上限。
2. 作为值班者，oldestInFlightAge 接近超时 → 即将重投潮，消费方健康先查。
3. 作为框架宿主，消息与确认口径自声明，纯记账不投递。

## Implementation Decisions

- 纯记账；边界双含上（到点即回队、穷尽即死信——不留侥幸窗）。

## Testing Decisions

- 重投边界（1299/1300/已确认）；死信边界（2/3、3/3、0/0）；三段普查
  四消息分账+最老龄；空/null 哨兵+畸形三型 fail-fast。

## Out of Scope

- 不执行投递/死信路由；不做退避重投（ReconnectBackoffLadder 已有阶梯）。

## Further Notes

- 与 WebhookOutbox 互补：那是 outbox 存取，这是投递时限账。
