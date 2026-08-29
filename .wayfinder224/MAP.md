# Wayfinder Map — Buzhou 事件 schema 检查器（effort #224，B 会话第 47 轮）

> B 会话第 47 轮。webhook 接收方按文档字段写消费代码，但事件 payload 无
> schema 约束——字段改名/缺失静默流到下游才炸。借鉴 JSON Schema 的 required
> 面（最小可用版：per-type 必备键集）。

## Destination

EventSchemaChecker（core/webhook）：声明 per-type 必备键集；check(event) →
违规列表（缺键）；装饰器模式拦截违规（计 violated + 丢弃 or 放行可配——
默认丢防止坏事件污染下游）；未声明类型放行（open-world）。

## Notes

- 号段：B=奇数 spec（本轮 209）；轮次 .wayfinder200+。
- 最小面（required 键集）——类型/枚举校验留档 JSON Schema 全量版。
- 与 20（信封契约）互补：那是传输层，这是 payload 语义层。

## Decisions so far

- 违规默认丢弃（fail-closed 保护下游）+ 计数；显式可配放行（调查期）。

## Not yet specified

- 类型/枚举校验；schema 声明 yml 化。

## Out of scope

- 沿用各轮；JSON Schema 全量；自动生成声明。

## Tickets

- [x] [T583 EventSchemaChecker（必备键集+拦截）](tickets/T583-schema-check.md)（impl-319）
- [x] [T584 schema 回归（缺键拦/全过/未声明放行/放行模式）](tickets/T584-schema-tests.md)（impl-319）
