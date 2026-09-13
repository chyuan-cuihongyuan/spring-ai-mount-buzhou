# 806 — 预算用量分位推荐

> 来源：H 会话第 7 轮 = effort #806 / [T1113](../../.wayfinder/tickets/T1113-budget-recommendation.md) / [T1114](../../.wayfinder/tickets/T1114-budget-recommendation-verify.md) / impl 559。
> 借鉴：k8s VPA（≈115K star）——观测用量分位 → resource requests 推荐。

## Problem

预算档位（token 上限/周期成本）靠拍脑袋：设小了频繁 hard-stop 破任务、设大了失控窗口大。观测到的用量分布没有被结构化为推荐依据。

## Solution

`BudgetRecommendation`（core.budget）：

- **分位**：最近秩 P50/P95/P99（确定性，排序后 ⌈p/100·n⌉ 取值）。
- **推荐**：⌈P95 × (1 + headroom%/100)⌉——headroom 显式传入（0..500 域外 fail-fast）。
- **诚实门槛**：样本 <5 → sufficient=false、recommended=-1 哨兵（不下结论）。
- **Ring**：容量 1024 FIFO 有界收集器 + dropped 计数（喂 token/成本微美元皆可——单位无关）。

## 兼容性

纯新增静态工具（喂样由应用/装配侧接 TokenBudgetHook 事件流）；无配置键。

## 诚实边界

最近秩非插值分位；单变量推荐（多维切片归调用方）；只读不自动调预算（VPA in-place/auto 留位）；样本单位由调用方定义。
