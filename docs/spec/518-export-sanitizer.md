# Spec 518 — 会话导出脱敏（effort #518）

> wayfinder map：`.wayfinder/maps/effort-518.md`（T787–T788）。E 会话第 19 轮。

## Problem Statement

28 导出产物含全部消息正文/摘要 sections/state 值——支持包/调试导出
直接分享即泄露 PII。86 PiiDetector 可复用（guard→core 方向合法），
缺一个导出级组合面。

## Solution

`guard.pii.SessionExportSanitizer`：

- `sanitize(SessionExport)` → 不可变副本：消息 content/reasoningContent
  脱敏重建、summary.sections 各段值脱敏、StateEntry.value 脱敏——
  占位符 `[PII:TYPE]`（86 同口径）+ customRules 叠加。
- 结构字段（sessionId/appId/agentName/时间戳/toolCalls/metadata）原样。
- 构造（types 可空=全类型，customRules 可空）。
- 与 510 组合：`seal(sanitizer.sanitize(export))` = 先脱敏再封缄。

## User Stories

1. 作为支持工程师，我想把问题会话导出发给框架方， so 导出包不含真实
   PII（占位符化）。
2. 作为集成方，我想保留自定义规则脱敏（订单号/工号）， so 领域敏感
   数据同样受控。

## Implementation Decisions

- 不可变副本：记录重建（原导出零改动——调用方可继续用原版入 510 封缄）。
- 脱敏域 = 内容域（content/reasoningContent/sections/state value）；
  结构域不碰。

## Testing Decisions

- 含 PII 的导出 → 副本占位符化且无原文；原导出零改动（字段比对）。
- custom rules 叠加；干净内容恒等；state/summary 各槽覆盖。

## Out of Scope

- toolCalls 参数 JSON；metadata；NLP 面。

## Further Notes

- 新公共类型 `SessionExportSanitizer` 随轮 regenerate 快照 +
  api-surface.md 加行。
