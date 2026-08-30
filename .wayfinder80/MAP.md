# Wayfinder Map — Buzhou 自定义 PII 规则（effort #80，50 轮自迭代第 45 轮）

> effort #80，延续 #79（T421–T422 / impl-264）。主线：**spec 86 fog「自定义
> recognizer SPI」最小版**——内置五型覆盖通用实体，订单号/工号/内部 token 等
> 领域格式每家不同；Presidio PatternRecognizer 的对应物。

## Destination

`CustomPiiRules`（Rule(name, pattern)：NAME 须 [A-Z0-9_]{2,32} fail-fast；ReDoS
风险归声明方 javadoc 显性）+ `PiiRedactionHook` 新 3 参构造（内置五型 + 自定义
叠加；占位符同形态 [PII:NAME]）；无规则构造零变化。

## Notes

- 借鉴：Presidio PatternRecognizer（命名正则实体）。

## Decisions so far

- 叠加不做占位符短路（幂等由规则特异性保证——短路会挡内置结果之上的自定义）。

## Not yet specified

- yml 声明式规则配置（buzhou.guard.pii.custom-rules——当前编程面；需求后议）；
  输入侧 hook 同款叠加。

## Out of scope

- 沿用 #7–#79。

## Tickets

- [x] [T425 CustomPiiRules + hook 3 参构造叠加](tickets/T427-custom-rules.md)（impl-265）
- [x] [T426 3 例红队（占位符/叠加+校验/零变化）+ PII 回归 + 收口](tickets/T428-custom-rules-close.md)
