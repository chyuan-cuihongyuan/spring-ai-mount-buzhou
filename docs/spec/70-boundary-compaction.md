# Spec 70 — 边界机会压缩（effort #30）

> wayfinder map：`.wayfinder30/MAP.md`（T291–T292）。OSS 借鉴：Letta「在自然边界
> 压缩」（research Tier2 遗留项）。

## Problem Statement

摘要只在预算压力下触发：压力态先走逐出梯子（有损微压缩加压）才折摘要——摘要生成
发生在上下文最紧张时（生成质量与预算双降）。Letta 洞察：应在自然停顿点（边界）压缩，
而非等到墙。

## Solution

opt-in `buzhou.memory.boundary-compact-backlog=N`（默认 0=关）：待摘积压 ≥ N 时在
轮边界提前增量摘要（预算尚宽松——摘要 token 预算充足、豁免梯子紧急态）。Completed-Turn
是自然边界的结构代理（真语义检测 fog 留位）。

## User Stories

1. 作为长会话用户，我要摘要提前在宽松态生成，所以摘要质量更高、避免梯子有损加压。
2. 作为红队，我要未达阈值行为零变化，所以默认路径不受影响。
3. 作为红队，我要无摘要模型/熔断开路时跳过提前摘要，所以降级口径与既有一致。

## Implementation Decisions

- backlog = toSummarize 同口径计数（cutoff 内、超过 coversUpTo、未摘）。
- 触发在预算判定前；阈值默认 0=关。

## Testing Decisions

- 积压达标提前出摘要块；未达标不提前；默认零变化。

## Out of Scope

- 语义漂移检测；字符/token 加权；新配置组。

## Further Notes

- 成本口径：提前摘要 = 一次摘要调用换梯子豁免（runbook 入档）。
