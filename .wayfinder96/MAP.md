# Wayfinder Map — Buzhou outbox 积压滞后面（effort #96，B 会话第 8 轮）

> B 会话第 8 轮。主题池「outbox 积压 age」：投递停摆现在要靠 due 审计（spec 96）
> 或翻死信才可见——缺 Kafka consumer-lag 式的<b>实时滞后读数</b>。

## Destination

WebhookOutboxLag：pending 计数 + 最老积压 age（含退避中——due() 看不到的部分）+
死信数 + stalled 阈值判定 + gauge 绑定。借鉴 Kafka consumer lag / Prometheus
`kafka_consumergroup_lag` 面向运维的直觉。

## Notes

- 号段：B=奇数 spec（本轮 135）。
- WebhookOutbox 增包内 pendingOldest（scanByPrefix min createdAt）——唯一共享
  文件触点（A 侧近期无 webhook 轮，风险低）。
- 诚实边界：scanByPrefix 全量读（容量有界 capacity 软上限——读放大可接受；
  count 走 countByPrefix 下推）。

## Decisions so far

- age 从 createdAt 起算（入队即计时——投递总滞后，非单次退避滞后）。

## Not yet specified

- autoconfig 定时采样入档；alert webhook 外发。

## Out of scope

- 沿用 #7–#95；多 sink 分目的地 lag（每 sink 一份）。

## Tickets

- [x] [T483 WebhookOutbox.pendingOldest + WebhookOutboxLag 读数](tickets/T483-outbox-lag.md)（impl-280）
- [x] [T484 lag 回归（age/退避含入/死信/stalled/gauge）](tickets/T484-outbox-lag-tests.md)（impl-280）
