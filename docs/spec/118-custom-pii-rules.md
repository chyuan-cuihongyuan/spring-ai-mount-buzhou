# Spec 118 — 自定义 PII 规则（effort #80）

> wayfinder map：`.wayfinder80/MAP.md`（T425–T426）。spec 86 fog「自定义 recognizer」
> 最小版；Presidio PatternRecognizer 对应物。

## Problem Statement

内置五型（spec 86）覆盖通用实体；订单号/工号/内部 token 等领域格式每家不同——
没有扩展面，领域 PII 防线只能改框架代码。

## Solution

`CustomPiiRules`（guard/pii）：
- `Rule(name, pattern)`——NAME 须 `[A-Z0-9_]{2,32}`（占位符形态稳定——违规
  fail-fast IllegalArgumentException）；正则由宿主自负（ReDoS 风险归声明方，javadoc
  显性告知）。
- `redact(text)`——命中替换 `[PII:<NAME>]`（与内置同形态）；叠加场景不做占位符
  短路（幂等由规则特异性保证）。
`PiiRedactionHook` 新 3 参构造（内置五型 + 自定义叠加）；既有构造零变化。

## User Stories

1. 作为宿主，我声明领域格式即得防线，不改框架代码。

## Testing Decisions

- 双规则占位符等值 + 叠加继续脱 + 无命中原文；hook 内置+自定义叠加；NAME 校验
  双 fail-fast；无规则构造行为不变。

## Out of Scope

- yml 声明式配置；输入侧叠加；NER 型规则。

## Further Notes

- 三层防线（内置/自定义/输入侧）就位——PII 防线家族完整。
