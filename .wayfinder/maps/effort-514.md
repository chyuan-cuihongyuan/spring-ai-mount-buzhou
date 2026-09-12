# Wayfinder Map — Buzhou 投递时延分位数（effort #514，E 会话第 15 轮）

> E 会话第 15 轮（416 分位族扩散轮）。勘察：webhook 投递面有 lag 滞后
> (135)/seq 围栏(159)/schema 门(307)——已投递事件的**时延分布**空白
> （135 lag 看还没送的，这里看送出去花了多久）。416 分位族同法。

## Destination

`webhook.WebhookDeliveryLatency`：成功投递时延样本滚动窗（有界 512，
Clock 无关——样本即事实）+ exact 最近秩 p50/p95/p99（零样本 null 诚实
空值——416 同口径）+ max/count。接线：WebhookEventForwarder DELIVERED
分支 setDeliveryLatency(setter——105 setIncludeTypes 同法，默认 null
零变化）记 now-createdAtEpochMs。诚实边界：只记成功投递样本（死信/
退避中不是「送达」）；重启清零（进程内观察面口径）。

## Notes

- 号段：spec 514 / T779–T780 / impl-417。
- 借鉴源：Prometheus histogram_quantile / Kafka end-to-end latency。

## Out of scope

- per-type 时延；持久化时延历史；自动告警（312 规则可接 snapshot）。

## Tickets

- [x] [T779 时延分位数原语](../tickets/T779-delivery-latency.md)
- [x] [T780 forwarder 接线](../tickets/T780-delivery-latency-assembly.md)
