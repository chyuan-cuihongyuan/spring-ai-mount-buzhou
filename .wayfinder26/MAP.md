# Wayfinder Map — Buzhou 前缀稳定注入序（effort #26）

> effort #26，延续 #5–#25（累计 172 轮 / T1–T282 / impl 1–211）。
> 主线：**注入序前缀稳定化（KV-cache 友好）**——注入块序为 摘要→事实→清单（最易变
> 在前，对 provider KV-cache 前缀命中最差）。借鉴 Anthropic prompt caching 最佳实践
> （稳定内容在前、易变在后；OpenAI automatic prefix caching 同理）：技能清单（跨轮
> 最稳定）前置。

## Destination

`buzhou.memory.prefix-stable-injection=true`（默认关）时注入块序切换
catalog→summary→facts→recent——清单块跨轮字节级一致（前缀稳定测试钉住）；默认关
块序零变化（spec 04 口径回归钉住）；新键 1 个登记 metadata 与矩阵。

## Notes

- 外部事实源：Anthropic prompt caching（系统提示词稳定前缀最大化缓存命中）；
  OpenAI automatic prefix caching（前缀精确匹配）。诚实边界：命中增益归 provider
  计费行为——框架只保证「稳定块前置」的结构事实；块语义（P0 死保/预算口径）不变。

## Decisions so far

- setter 注入（构造器涟漪规避——与 factReconciliation 同先例）；单一拼装点
  assembleWithSummary 分支。
- 摘要与事实块内部顺序不动（P0 语义/预算入账不受影响）。

## Not yet specified

- 摘要块的轮间增量形态（换摘要仍断前缀——结构性限制诚实入档）；facts 稳定化排序。

## Out of scope

- 沿用 #7–#25；块内容自身的变化频率治理；provider 特定 cache_control 头。

## Tickets

- [x] [T283 前缀稳定序开关 + 拼装分支 + yml 键](tickets/T283-prefix-order.md)（impl-212）
- [x] [T284 测试（跨轮首块字节一致/默认序回归）+ metadata/矩阵 + verify + 收口](tickets/T284-prefix-close.md)
