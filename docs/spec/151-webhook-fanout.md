# Spec 151 — 多 sink webhook 扇出（effort #104）

> wayfinder map：`.wayfinder104/MAP.md`（T507–T508）。借鉴：Kafka 多消费者组
> （同事件流多组独立消费）/ CloudEvents 多协议分发。

## Problem Statement

会话事件的外发 webhook 只有一个目的地：审计系统要全量、告警系统只要错误族、
业务回调只要本租户事件——三类订阅者共用一个 url 意味着互相过滤互相干扰，
一个 sink 的积压/重试还挤占别人的投递预算（共享 outbox）。

## Solution

`WebhookFanout`（core/webhook，implements SessionEventListener）：

- **扇出**：持 N 个 `WebhookEventForwarder` sink（各自 url / secret /
  include-types / outbox / 退避 / 死信<b>完全独立</b>）；onEvent 广播全部，
  各 sink 按自己的类型过滤（入队前滤——被滤不占容量，spec 105 语义逐 sink 生效）。
- **隔离**：每 sink 独立 stateStore（outbox 独立——一 sink 积压不影响另一 sink
  的容量与退避）；close() 全关（逐 sink 排空语义保持）。
- **零变化**：不用 fanout = 单 forwarder 原样；fanout 不引入新投递逻辑
  （签名/幂等/退避/死信全部复用 forwarder 既有实现）。

## User Stories

1. 作为宿主，审计（全量）、告警（错误族 include-types）、业务回调（会话族）
   三类订阅者各得其所——一个事件流三份订阅互不干扰。
2. 作为运维，告警 sink 宕机积压不影响审计 sink 投递（outbox 隔离）。
3. 作为宿主，单 sink 场景零改动（fanout 是纯增量装配位）。

## Implementation Decisions

- fanout 不缓存不重排——纯广播（顺序由各 sink 自持 outbox seq 保证）。
- sink 列表构造期确定（运行期增减 sink 留档——热订阅面后续）。

## Testing Decisions

- 双 HttpServer 收件：类型路由（sink1 只要 turn.completed——另一事件不达）/
  全投 sink2 两件全收 / 一 sink 停机（404）不影响另一 sink 送达 / close 后停发。
- 先例：WebhookEventForwarderTest（JDK HttpServer + Collector）。

## Out of Scope

- 跨 sink 全序；运行期动态增删 sink；yml 多目的地配置面。

## Further Notes

- webhook 家族：单投递（20/24/79/105）→ 滞后面（135）→ 多目的地（本轮）。
