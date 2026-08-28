# Wayfinder Map — Buzhou agent 级成本归集（effort #25）

> effort #25（已闭合 2026-08-29），延续 #5–#24；收口后累计 172 轮 / impl 1–211。
> 主线：**agent 级成本归集**——budget 是 per-session 累计；「这个 agent 本月烧了多少」
> 需要跨会话 rollup。借鉴 LiteLLM spend tracking（按 team/project/tag 聚合支出）；
> 本仓适配为 per-agentName 合成会话台账（state store 命名空间 + AtomicStateCounters
> 跨实例原子累计）。

## Destination

`AgentCostLedgerHook`（宿主显式挂载，零自动装配零新键）：afterModel 按 agentName 把
usage（prompt/completion tokens + 定价 microUsd）累计进合成会话 `__buzhou.cost__`
（CAS 原子跨实例）；`AgentCostLedger.query()` 按前缀扫描出 per-agent 台账
（tokens/microUsd/会话无关）；红队钉并发精确与 agent 隔离。

## Notes

- 外部事实源：LiteLLM spend tracking（team/project 维度支出聚合 + 预算挂钩）。
- 复用面：AtomicStateCounters（#22）、TokenBudgetHook 的 usage 提取与定价口径。
- 诚实边界：agentName 粒度（appId 不在 HookContext——fog 留位）；台账键净化防注入。

## Decisions so far

- 显式挂载（不自动装配）——隐藏写放大不可接受；查询走 scanByPrefix（OLAP 导出面同源）。
- agent 名净化入键（[^A-Za-z0-9._-] → _，与 Redis 键净化同纪律）。

## Not yet specified

- appId/tag 维度（HookContext 扩面后议）；台账 TTL/重置（运维手工删合成会话键即可）。

## Out of scope

- 沿用 #7–#24；自动装配与新键；与 budget 硬顶联动（台账只记账不拦截）。

## Tickets

- [x] [T281 AgentCostLedgerHook + query 面](tickets/T281-cost-ledger.md)（impl-211）
- [x] [T282 红队（并发精确/agent 隔离/定价/查询）+ 文档 + verify + 收口](tickets/T282-cost-close.md)（impl-211；4 例 ×3 轮；累计 172 轮）
