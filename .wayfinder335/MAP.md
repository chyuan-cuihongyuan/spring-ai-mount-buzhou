# Wayfinder Map — Buzhou 成本归因台账（effort #334，C 会话第 35 轮）

> C 会话第 35 轮。成本账两轴已立：per-model 全局账（174/ModelCostLedger）
> 与 per-session 会话闸（TokenBudgetHook）。但「哪个虚拟 key 在烧钱」
> ——chargeback 的归因问题——两轴都答不了：Kubecost/OpenCost 的核心
> 思想正是按标签（namespace/label）归因成本而非只按资源聚合。

## Destination

`CostAttributionLedger`（core.budget：双维归因 MODEL/VIRTUAL_KEY——
同一笔 microUsd 同时入两维账；无 key 归 __unattributed__ 诚实桶；维值
封顶 64 折 __overflow__（ModelCostLedger 同纪律）；share 百分比 +
稳定排序 + reset 窗口循环）+ `CostAttributionJsonl`（导出族新员：一行
一归因行含 usd/sharePercent）+ 打点（TokenBudgetHook afterModel——
model/virtualKey/cost 三元组已在手，恰是喂点）。

## Notes

- 号段：spec 334 / T659–T660 / impl-357。
- 借鉴源：Kubecost / OpenCost（按标签归因 + chargeback 报表）。
- 纪律：零成本也记（「跑过零成本」是归因事实）；只记账不拦截。

## Decisions so far

- 归因维先开两维（MODEL/VIRTUAL_KEY）——维度枚举开放后续（skill/tool）。
- share 用万分比整数（basis points）——无浮点漂移（microUsd 同口径哲学）。

## Out of scope

- 实时 chargeback 拦截（虚拟 key 限额已有 315）；skill/tool 维归因
  （喂点缺上下文——后续轮接）；持久化账仓（export→reset 窗口纪律即答案）。

## Tickets

- [x] [T659 CostAttributionLedger 双维归因](tickets/T659-attribution-ledger.md)
- [x] [T660 JSONL 导出 + 打点接线 + 收口](tickets/T660-attribution-export.md)
