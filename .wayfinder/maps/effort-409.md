# Wayfinder Map — Buzhou 工具结果 schema 校验（effort #409，D 会话第 10 轮）

> D 会话第 10 轮。勘察（2026-09-08）：入参有 ToolArgsValidator（执行前
> 校验 + REASK 反馈），**工具结果零校验**——工具坏输出（缺键/类型错/非
> JSON）原样回灌模型，模型基于残缺数据继续推理（错上加错不可区分）。
> MCP 规范的 outputSchema 方向在此无对应物。

## Destination

`core.exec.ToolResultSchemaHook`（MCP outputSchema 借鉴）：afterTool——
per-tool 结果 schema（yml `buzhou.tools.result-schemas.<name>` = 最小
子集 JSON schema 串，**复用 ToolArgsValidator 同一校验器**——零新校验
逻辑）；违例 replaceResult 为结构化标记反馈（ToolFeedbackType 同词汇
表新增 RESULT_SCHEMA_FAILURE? 不——复用 VALIDATION_FAILURE 词汇档：
「结果未过 schema」与「参数未过」同属校验档，但文案区隔）+ 事件 +
计数；通过则零改写。opt-in per-tool 未声明零变化。

## Notes

- 号段：spec 409 / T709–T710 / impl-382。
- 借鉴源：MCP（Model Context Protocol，GitHub 50k★）outputSchema——
  工具声明输出契约；Pydantic AI 校验失败转反馈。
- 纪律：校验器复用（不 fork 第二套 JSON 校验）；反馈走既有标记词汇
  （观测/预算两档语义不改）；结果 schema 的宽容口径与入参同
  （未知关键字忽略、schema 缺失放行）。

## Out of scope

- 结果自动修复（transform 族已管）；per-tool 宽严模式；schema 热更新
  （yml 刷新族候选）；流式工具结果（工具面无流式）。

## Tickets

- [x] [T709 ToolResultSchemaHook](../tickets/T709-result-schema-hook.md)
- [x] [T710 yml 装配 + E2E](../tickets/T710-result-schema-assembly.md)
