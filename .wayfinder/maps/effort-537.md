# Wayfinder Map — Buzhou 死信原因分类计数（effort #537，E 会话第 37 轮）

> E 会话第 37 轮（135 死信面观测扩散轮）。勘察：死信计数只有总量
> （buzhou.webhook.dead-letter/deadCount）——**原因分类**（4xx 永久/
> 重试耗尽暂时）不可分：接收端配置错（404/401）与瞬时故障治理动作
> 不同。

## Destination

forwarder markDead 增 `buzhou.webhook.dead-reason` 计数（tag reason
有界：4xx | 重试耗尽）——治理动作分流的数据面。

## Notes

- 号段：spec 537 / T827–828 / impl-438（顺延）。

## Out of scope

- 死信 reason 持久化入记录；自动重放策略。

## Tickets

- [x] [T827 原因分类计数](../tickets/T827-dead-reason-counter.md)
- [x] [T828 forwarder 接线](../tickets/T828-dead-reason-forwarder.md)
