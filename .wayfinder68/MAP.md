# Wayfinder Map — Buzhou 用户输入 PII 脱敏（effort #68，50 轮自迭代第 33 轮）

> effort #68，延续 #67（T389–T390 / impl-252）。主线：**spec 86 fog 项「用户输入
> 侧脱敏（beforeTurn）」**——输出侧（工具返回）有了，用户误贴的身份证/卡号仍直进
> prompt 与观测日志。

## Destination

`PiiInputRedactionHook`（beforeTurn，order 60）：输入命中类型 → `ctx.replaceInput`
占位符化（[PII:TYPE]）再进模型；幂等（占位符前缀短路）；类型子集复用输出侧
PiiDetector；计数器 buzhou.guard.pii.input-redactions（tag type）。装配：
Builder.piiInputRedaction() + `buzhou.guard.pii.input-redaction`（默认关，types
与输出侧共用）。与输出侧正交（两个独立开关）。

## Notes

- 借鉴：Presidio 实体类型面（同 #47——输入通道补齐）。

## Decisions so far

- 改写而非拦截（占位符保留可读性——用户意图不丢；拦截语义过严）。

## Not yet specified

- 输入侧类型子集独立配置（当前共用 pii.types——需求证据后议）。

## Out of scope

- 沿用 #7–#67。

## Tickets

- [x] [T391 PiiInputRedactionHook + 装配独立开关](tickets/T393-pii-input.md)（impl-253）
- [x] [T392 2 例红队（改写/幂等+子集+零改写）+ guard 回归 + 收口](tickets/T394-pii-input-close.md)
