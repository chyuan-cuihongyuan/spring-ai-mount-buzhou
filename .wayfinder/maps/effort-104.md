# Wayfinder Map — Buzhou 多 sink webhook 扇出（effort #104，B 会话第 16 轮）

> B 会话第 16 轮。主题池「多 sink webhook」：事件现在只能投一个目的地——
> 审计系统、监控告警、业务回调各要一份时无解。借鉴 Kafka 多消费者组 /
> CloudEvents 多协议分发。

## Destination

WebhookFanout（core/webhook）：N 个 WebhookEventForwarder sink（各自
url/secret/include-types/outbox/退避/死信独立），onEvent 扇出到全部；
close 全关。单 sink 语义零变化（不包 fanout 即原样）。

## Notes

- 号段：B=奇数 spec（本轮 151）。
- 复用 forwarder 全部既有语义（签名/幂等/退避/死信/类型过滤入队前）——
  fanout 只做扇出编排，零新投递逻辑。

## Decisions so far

- 每 sink 独立 stateStore（outbox 隔离——一 sink 积压不挤占另一 sink）。

## Not yet specified

- autoconfig 多目的地 yml 配置面。

## Out of scope

- 沿用 #7–#103；跨 sink 顺序保证（各自独立序——Kafka 组间亦如此）。

## Tickets

- [x] [T507 WebhookFanout 扇出编排](../tickets/T507-webhook-fanout.md)（impl-288）
- [x] [T508 扇出回归（全投/类型路由/独立积压/关闭）](../tickets/T508-fanout-tests.md)（impl-288）
