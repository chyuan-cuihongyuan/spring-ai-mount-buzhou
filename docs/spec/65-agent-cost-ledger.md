# Spec 65 — agent 级成本归集（effort #25）

> wayfinder map：`.wayfinder/maps/effort-25.md`（T281–T282）。OSS 借鉴：LiteLLM spend
> tracking（team/project 支出聚合）。

## Problem Statement

token/成本预算是 per-session 累计；回答「某 agent 跨会话总消耗」需要逐会话手加——
成本归因粒度缺一层 rollup。

## Solution

`AgentCostLedgerHook`（宿主显式挂载）：afterModel 把 usage 按 agentName 累计进合成会话
`__buzhou.cost__` 的 state 键（prompt/completion tokens + 定价 microUsd；CAS 跨实例
原子）；`AgentCostLedger.query()` 前缀扫描出 per-agent 台账行。只记账不拦截（预算硬顶
仍归 budget hook）。

## User Stories

1. 作为运维者，我要按 agent 聚合 token/成本，所以成本归因与配额规划有 rollup 层。
2. 作为多实例运维者，我要台账累计跨实例原子，所以总额不被并发写覆盖。
3. 作为红队，我要 agent 间隔离与键净化，所以命名注入与串账被钉死。
4. 作为既有用户，我要不挂载零写放大，所以默认零行为变化。

## Implementation Decisions

- 合成会话 `__buzhou.cost__`（fsck 白名单同源约定）；键 `agent.<净化名>.<维度>`。
- usage 提取与 microUsd 定价复用 TokenBudgetHook 同口径（props 注入）。
- 查询：scanByPrefix("agent.") → AgentCostRow(agentName, promptTokens, completionTokens, microUsd)。

## Testing Decisions

- 并发红队（双实例累计精确）、agent 隔离、定价换算、查询解析、无挂载零写。

## Out of Scope

- 自动装配/新键；budget 联动拦截；appId/tag 维度。

## Further Notes

- 台账重置 = 删合成会话键（运维面既有能力）。
