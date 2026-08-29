# Wayfinder Map — Buzhou 事件载荷出站脱敏（effort #207，B 会话第 30 轮）

> B 会话第 30 轮。PII 防线观察：输入/输出两侧已脱敏（86/106/118/129），但
> <b>事件出站面漏了</b>——session 事件 payload（如 turn 输入原文）经 webhook
> 发往外部系统，PII 随之外流。防线要盖住最后一个出口。

## Destination

PiiEventRedactor（guard/pii，SessionEventListener 装饰器）：onEvent →
payload 的 String 值过 PiiDetector（内置五型）+ CustomPiiRules（叠加），
构造脱敏后新事件（type/occurredAt 不变；非 String 值原样——数字布尔天然
安全）再下发。零配置包装即用。

## Notes

- 号段：B=奇数 spec（本轮 177）；轮次 .wayfinder200+。
- 与 webhook fanout（151）组合：sink 包装 redactor 即全站出站脱敏。
- 类型选择面：per-event-type 跳过表（遥测类事件全数值无需脱敏——性能短路）。

## Decisions so far

- 深度仅一层 payload（Map 值为 String 才处理）——嵌套结构递归留档。

## Not yet specified

- 嵌套 Map/List 递归脱敏；跳过表配置化。

## Out of scope

- 沿用各轮；观测存储内部脱敏（入站面另议）。

## Tickets

- [x] [T547 PiiEventRedactor（出站事件载荷脱敏装饰器）](tickets/T547-event-redactor.md)（impl-302）
- [x] [T548 出站脱敏回归（电话脱/自定义叠加/非串原样/透传）](tickets/T548-event-redactor-tests.md)（impl-302）
