# Wayfinder Map — Buzhou 结构化输出执法（effort #402，D 会话第 3 轮）

> D 会话第 3 轮。勘察（2026-09-08）：eval 族有 DatasetExpectations（离线
> 评测口径），guard 有 ToolArgsValidator（工具入参 schema）——但**模型终态
> 输出无任何运行时 JSON 契约执法**。「让模型输出 JSON」目前只能靠提示词
> 恳求，坏输出直接漏到宿主解析层炸 NPE/JSONException。

## Destination

`resilience.structured` 包（instructor 借鉴——验证失败把错误喂回模型重试）：
`OutputSchema`（最小子集 JSON 契约：required 键 + 键类型表；代码围栏剥离）
+ `StructuredOutputAdvisor`（order +480，缓存内/观测外；首答走链、修复直达
> 模型终端——链为单遍弹出 Deque，ResilienceAdvisor 内层重试同口径）；
验证失败→结构化反馈 UserMessage
> 重调，`max-repair-attempts`（默认 1，0=纯执法）耗尽抛
> `StructuredOutputViolationException`；流式直通为诚实边界）+
`StructuredOutputProperties`（`buzhou.resilience.structured-output.{enabled,
max-repair-attempts,schema.{required,properties}}`，enabled 而无 schema
fail-fast）。独立 RuntimeConfig bean 装配（不动 ResilienceModule 内路）。

## Notes

- 号段：spec 402 / T695–T696 / impl-375。
- 借鉴源：instructor（10k★）——「验证错误作为消息回灌、模型自修复」；
- 事件三枚：structured-output.repair-attempted / repaired / violation。
- 纪律：契约子集（object/required/类型）文档化为最小面——完整 JSON
  Schema 引擎另议；修复只走 adviseCall（流式聚合后无法重调）。

## Out of scope

- 完整 JSON Schema Draft 引擎；流式执法；per-prompt 契约（提示词注册表
  元数据扩散候选）；模式宽容度（additionalProperties 禁止类）。

## Tickets

- [x] [T695 OutputSchema + 修复环 Advisor](../tickets/T695-structured-output-advisor.md)
- [x] [T696 yml 装配 + E2E](../tickets/T696-structured-output-assembly.md)
