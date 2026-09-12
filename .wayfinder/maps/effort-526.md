# Wayfinder Map — Buzhou 水位告警桥接（effort #526，E 会话第 27 轮）

> E 会话第 27 轮（181×312 桥接轮）。勘察：181 水位是 per-session 事件
> +全局 gauge——312 告警引擎按**机制健康面**订阅：「低水位会话数达阈值」
> 无机制健康面（事件逐会话刷屏、gauge 无裁决）。

## Destination

ContextWatermarkHook 观测访问器（lowWaterSessionCount/isEnabled）+
`health.WatermarkHealth implements BuzhouHealth`（mechanism=
context-watermark）：低水位会话数 ≥ 阈值 → DOWN（312 规则按机制名+
for 窗口订阅——聚合裁决面与 181 逐会话事件面分层）；hook 未启用=UP
（无数据不告警，disabled 详情）。

## Notes

- 号段：spec 526 / T805–806 / impl-429。
- 借鉴源：Google SRE（容量压力信号进告警面）。

## Out of scope

- per-session 告警；自动扩容联动。

## Tickets

- [x] [T805 聚合健康面](../tickets/T805-watermark-health.md)
- [x] [T806 阈值与禁用语义](../tickets/T806-watermark-health-semantics.md)
