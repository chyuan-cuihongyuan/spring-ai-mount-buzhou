# Wayfinder Map — Buzhou span 状态分布读数（effort #543，E 会话第 43 轮）

> E 会话第 43 轮（519 同包扩散轮；Prometheus label 聚合思想）。勘察：
> TOOL/TURN/MODEL span 的 kind×status 分布无读数面（412 只按 epoch 桶
> 聚合 calls/errors）。

## Destination

`observability.analytics.SpanStatusDistribution`（纯函数+store 重载）：
kind×status 计数（status 大小写归一、null→UNSET；TreeMap 输出稳定）。

## Notes

- 号段：spec 543 / T841-842 / impl-445。

## Out of scope

- 跨会话聚合；时间维（412 已有桶维）。

## Tickets

- [x] [T843 span 状态分布原语](../tickets/T843-span-status-distribution.md)
- [x] [T844 store 便捷重载](../tickets/T844-span-status-store.md)
