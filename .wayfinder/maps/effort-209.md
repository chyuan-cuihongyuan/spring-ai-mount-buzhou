# Wayfinder Map — Buzhou 上下文余量水位（effort #209，B 会话第 32 轮）

> B 会话第 32 轮。压缩在预算线触发（事后救火），但「窗口还剩多少」没有实时
> 水位——低水位要靠撞线才知道。借鉴水库水位预警（low-watermark alert）。

## Destination

ContextWatermarkHook（core/hook）：beforeModel 估算本轮注入字符量 vs
配置窗口字符容量——gauge buzhou.context.utilization；跌破低水位（默认 20%）
发一次 session 事件（翻转制不刷屏）+ 计数。纯观测不裁决。

## Notes

- 号段：B=奇数 spec（本轮 181）；轮次 .wayfinder200+。
- 字符口径估算（ContextWindowResolver 的 token 精算归装配侧——hook 零依赖）；
  容量由宿主配置（按「字符≈token×4」自换算）。
- 与压缩触发（预算线）互补：那是动作线，这是预警线。

## Decisions so far

- 翻转制低水位事件（跨线才发一次）。

## Not yet specified

- token 精算接线；autoconfig 配置面。

## Out of scope

- 沿用各轮；自动触发压缩（动作归既有机制）。

## Tickets

- [x] [T553 ContextWatermarkHook（利用率 gauge+低水位翻转事件）](../tickets/T553-watermark.md)（impl-304）
- [x] [T554 水位回归（gauge/低水位事件/恢复翻转/零配置安全）](../tickets/T554-watermark-tests.md)（impl-304）
