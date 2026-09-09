# Spec 86 — 工具输出 PII 脱敏（effort #47）

> wayfinder map：`.wayfinder/maps/effort-47.md`（T331–T332）。借鉴：Microsoft Presidio
> （规则式 recognizer 子集——无 ML 依赖）。

## Problem Statement

外部数据（工具/RAG 返回）回灌上下文前有注入防御（spotlight/canary/taint），但无
PII 面：邮箱/手机号/身份证/银行卡号随工具输出直进 prompt、观测 span 与日志——
合规与泄漏风险。

## Solution

`PiiType`（EMAIL/CN_PHONE/CN_RESIDENT_ID/BANK_CARD/IPV4）+ `PiiDetector`（规则式
扫描；身份证 GB 11643 校验位、卡号 Luhn、IPv4 0-255 段验证收窄误报；重叠去重）
+ `PiiRedactionHook`（afterTool：命中类型替换 `[PII:TYPE]` 占位符；order 70 先于
spotlight 80；幂等（占位符前缀短路）+ error 跳过 + 计数器
`buzhou.guard.pii.redactions`（tag type——5 值有界））。装配：
`GuardModule.builder().piiRedaction(types?)` / `buzhou.guard.pii.enabled`（默认
false）+ `buzhou.guard.pii.types`（List/CSV）。

## User Stories

1. 作为合规负责人，我要工具输出 PII 不进 prompt/日志，所以泄漏面收窄到数据源侧。
2. 作为宿主，我要校验位收窄误报，所以订单号/时间戳不被误脱敏破坏可用性。
3. 作为运维，我要计数器按类型，所以 PII 命中趋势可观测。

## Implementation Decisions

- 先脱敏再 spotlight（order 70 < 80——两层互不依赖，脱敏作用于原文）。
- 规则式零依赖（无 ML）——NER 面（姓名/地址）fog 记账不预设。

## Testing Decisions

- 五型检出 + 坏校验位身份证/坏 Luhn 卡号/订单号时间戳不误杀；
- 多型混排占位符等值 + 类型子集 + 无命中引用等零改写；
- hook 幂等/error 跳过/无命中零改写；fromYml enabled+types 解析。

## Out of Scope

- 用户输入侧脱敏（beforeTurn）；NER 实体；列级存储脱敏。

## Further Notes

- 与 taint/spotlight 组成外部数据三层：脱敏（内容）→ 包裹（指令隔离）→ 污染
  标记（信息流）。
