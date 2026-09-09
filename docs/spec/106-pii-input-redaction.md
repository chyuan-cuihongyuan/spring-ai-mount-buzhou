# Spec 106 — 用户输入 PII 脱敏（effort #68）

> wayfinder map：`.wayfinder/maps/effort-68.md`（T391–T392）。spec 86 fog 项收口。

## Problem Statement

PII 脱敏（spec 86）只覆盖工具输出侧：用户在输入里误贴的身份证/卡号/邮箱仍直进
prompt、观测 span 与日志——合规风险在输入通道裸奔。

## Solution

`PiiInputRedactionHook`（guard/pii，order 60，beforeTurn）：输入命中启用类型 →
`TurnContext.replaceInput` 占位符化（`[PII:TYPE]`，PiiDetector 复用——校验位收窄
同款）；幂等（占位符前缀短路）；计数器 `buzhou.guard.pii.input-redactions`（tag
type 有界）。装配：`GuardModule.builder().piiInputRedaction()` /
`buzhou.guard.pii.input-redaction=true`（默认关；类型集与输出侧共用
`buzhou.guard.pii.types`）。与输出侧（spec 86）正交——两个独立开关。

## User Stories

1. 作为合规负责人，我要用户误贴的 PII 不进模型，所以输入通道泄漏面收窄。
2. 作为用户，我要脱敏不拦截（占位符保留意图），所以正常对话不被打断。

## Implementation Decisions

- 改写而非拦截（占位符保留可读性——TurnContext.replaceInput 既有能力）。
- 类型集共用（输入侧独立子集 fog 记账）。

## Testing Decisions

- beforeTurn 改写等值；幂等/类型子集/无命中引用等零改写。

## Out of Scope

- 输入侧独立类型子集；NER 面；结构化字段脱敏。

## Further Notes

- 输入（用户给）/输出（外部数据）双侧齐备——PII 防线闭环。
