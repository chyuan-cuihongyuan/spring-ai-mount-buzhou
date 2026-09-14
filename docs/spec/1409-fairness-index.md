# 1409 — 多租户配额公平指数

> 来源：L 会话第 10 轮 = effort #1409（票 T2119 / T2120 / impl 1062）。**换题记录**：原题「令牌桶水位读面」勘察发现 InMemoryRateLimitBackend.available() 已暴露桶余额、RateLimitKeyHotspot（H 837）覆盖热点轴——半撞换入 R20 题。借鉴：Kafka client quota（多租户配额的公平性治理——配额先要回答「用量在租户间是均匀还是倾斜」）。

## Problem Statement

会话/请求/token 用量在租户（appId/agent）间的分布无公平性度量：单租户吃满预算、其余饿死（noisy neighbor）只能从账本明细人肉聚合发现。RouteDistributionReadout（H 805）是路由「声明 vs 实际」比对（gini），非用量分布公平性口径。

## 目标

- `FairnessIndex`（core/budget，纯函数静态面，private 构造）：
  - `of(Map<String,Long> usageByTenant)` → `record FairnessReport(consumers, total, jain, dominantShare, shares)`；
  - Jain 公平指数 `J = (Σx)²/(n·Σx²) ∈ (0,1]`（1=完全均匀；FAIR_FLOOR=0.9 电信配额惯例 → 派生 `isFair()`）；
  - `dominantShare`（最大租户占比——单点吃满检测）+ `shares` 逐租户份额降序（平局按名典序——行动面）；
  - 哨兵：全零样本/空输入 jain=-1（全零无从谈公平，不冒充 1.0）；
  - `of(long[])` 便捷重载（位置命名 consumer-N）。
- x 轴口径由调用方声明（会话数/请求量/token 皆可）——纯函数不采样不接线。

## 兼容性

纯函数零状态零 IO；不触配额执行面（GcraRateLimitBackend/预算闸语义不变）。

## Out of Scope

- 用量采样接线（CostAttributionLedger 聚合面归既有机制）。
- 配额自动再平衡（读面不裁决）。
- gini 集中度（RouteDistributionReadout H 805 已有，不重复造）。
