# Wayfinder Map — Buzhou 会话导出脱敏（effort #518，E 会话第 19 轮）

> E 会话第 19 轮（28 导出 × 86 检测器组合轮；510 密文封缄是保密面，
> 本轮是隐私面——对外分享支持包/调试导出时 PII 不随行）。勘察：导出
> 产物（SessionExport）含全部消息正文/摘要 sections/state 值——直接
> 分享即泄露 PII；86 PiiDetector 规则式检测器可复用（guard 依赖 core
> 方向合法——SessionExport 是 core 类型）。

## Destination

`guard.pii.SessionExportSanitizer`：`sanitize(SessionExport)` → 新
导出副本——BuzhouMessage.content/reasoningContent 重建（PiiDetector
redact+customRules 叠加）、StructuredSummary.sections 各段值脱敏、
StateEntry.value 脱敏；sessionId/appId/agentName/时间戳/toolCalls 等
结构字段原样（结构字段无 PII 语义）。构造（types 可空=全类型、
customRules 可空）。不可变副本语义：原导出零改动（记录重建）。
诚实边界：toolCalls 参数 JSON 内文本不脱敏（结构化参数域——82 同
「参数域另议」注记）；metadata 值不脱敏（框架元数据非内容）。

## Notes

- 号段：spec 518 / T787–T788 / impl-421。
- 借鉴源：Presidio anonymize（86 同源）+ 支持包脱敏实践。
- 与 510 组合：sanitize → seal（先脱敏再封缄=对外分享全链）。

## Out of scope

- toolCalls 参数 JSON 深度脱敏；metadata 值脱敏；NLP 面。

## Tickets

- [x] [T787 消息/摘要/state 三槽脱敏](../tickets/T787-export-sanitizer.md)
- [x] [T788 不可变副本与组合面](../tickets/T788-export-sanitizer-semantics.md)
