# Spec 409 — 工具结果 schema 校验（effort #409）

> wayfinder map：`.wayfinder/maps/effort-409.md`（T709–T710）。D 会话第 10 轮。

## Problem Statement

入参有校验（ToolArgsValidator + REASK），工具结果零校验：坏输出（缺键/
类型错/非 JSON）原样回灌模型——模型基于残缺数据继续推理，错上加错
不可区分；下游宿主解析也只能自己兜。

## Solution

`core.exec.ToolResultSchemaHook`（MCP outputSchema 借鉴）：

- **afterTool**（观察+改写位）：per-tool 结果 schema 声明即校验——
  **复用 `ToolArgsValidator.validate(schemaJson, resultJson)` 同一校验器**
  （最小子集、未知关键字忽略、schema 不具结构放行——零新校验逻辑）。
- **违例处理**：`replaceResult` 为结构化标记反馈（复用
  `ToolFeedbackType.VALIDATION_FAILURE` 标记——校验档词汇，文案区隔
  「结果未过 schema，工具已执行」）；事件
  `tool.result-schema.violated`（tool + 原因）+ 计数器
  `buzhou.tools.result-schema-violations`（tag tool 有界=yml 声明集）。
  模型收到反馈可换参重调或弃用该工具（REASK 同语义，重试受 Turn 预算
  约束）。
- **通过零改写**（引用等）；工具无声明/结果 null/error 路径不校验。
- 装配：yml `buzhou.tools.result-schemas.<toolName>` = schema JSON 串
  （map 形态，Binder 预绑 Condition 判非空——406 同法）。

## User Stories

1. 作为宿主开发者，我想声明工具输出契约，so 坏输出在回灌模型前被
   拦下转为结构化反馈。
2. 作为模型，我想结果违例时收到可行动的反馈，so 我能换参重试或
   改道而不是基于残缺数据瞎猜。
3. 作为运维，我想违例按工具计数，so 哪个工具的输出契约最不稳定可见。

## Implementation Decisions

- 反馈走既有 VALIDATION_FAILURE 词汇档（观测/预算两档语义不变）；
  文案区隔「结果（已执行）」vs「入参（未执行）」。
- 非字符串结果：String.valueOf 后校验（JSON 对象工具的典型面）。

## Testing Decisions

- 通过零改写（引用等）；缺必备键/类型错/非 JSON 各自反馈；
- 无声明/错误路径不校验；反馈含标记与原因；
- yml map 装配 + 默认关。

## Out of Scope

- 结果自动修复；宽严模式；schema 热更新。

## Further Notes

- 新公共类型 `ToolResultSchemaHook` / `BuzhouToolResultSchemasProperties`
  随轮 regenerate 快照。
