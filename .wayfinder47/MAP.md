# Wayfinder Map — Buzhou 工具输出 PII 脱敏（effort #47，50 轮自迭代第 12 轮）

> effort #47，延续 #46（T327–T328 / impl-232）。主线：外部数据（工具/RAG 返回）
> 回灌上下文前只有注入防御（spotlight/canary/taint），无 PII 面——邮箱/手机号/
> 身份证/卡号随工具输出直进 prompt 与观测日志。Presidio 的规则式子集是零依赖
> 最小内核。

## Destination

`PiiType`（5 型）+ `PiiDetector`（规则式扫描：GB 11643 身份证校验位 + Luhn 卡号 +
IPv4 段验证收窄误报；重叠去重）+ `PiiRedactionHook`（afterTool 占位符
`[PII:TYPE]` 改写，order 70 先于 spotlight 80，幂等 + error 跳过 + 计数器
buzhou.guard.pii.redactions tag type）；GuardModule.Builder.piiRedaction(types?) +
fromYml `buzhou.guard.pii.enabled/types`（默认关）。零依赖（无 ML）。

## Notes

- 借鉴：Microsoft Presidio（规则 recognizer + 校验位收窄；NER 面在 fog 不预设）。

## Decisions so far

- 校验位/Luhn/段验证是误报收窄的第一道（长数字串 ≠ PII）；顺序：先脱敏再包裹
  （脱敏的是原文，包裹的是已脱敏文本——纵深两层互不依赖）。

## Not yet specified

- NER 型实体（姓名/地址——需模型或 ONNX）；用户输入侧脱敏（beforeTurn——需求
  证据后议）；自定义 recognizer SPI。

## Out of scope

- 沿用 #7–#46；结构化存储字段的列级脱敏（存储加密另议）。

## Tickets

- [x] [T331 PiiType/PiiDetector/PiiRedactionHook + Builder/fromYml](tickets/T331-pii-redaction.md)（impl-233）
- [x] [T332 4 例红队（五型+校验位收窄/多型+子集/hook 端到端/fromYml）+ 收口](tickets/T332-pii-close.md)
