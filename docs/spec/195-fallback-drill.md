# Spec 195 — 降级链演练（effort #216）

> wayfinder map：`.wayfinder216/MAP.md`（T567–T568）。借鉴：Envoy 主动健康
> 检查——备胎平时验证，用时可靠。

## Problem Statement

备模型降级链「存在」≠「可用」：凭证过期/配额耗尽/区域故障都是静默劣化——
平时（主模型健康）从不调用备模型，真降级那一刻才发现备胎瘪了。离群驱逐
（149）只对<b>被调用过</b>的模型有信号；从未轮到的备模型零信号。

## Solution

`FallbackDrill`（resilience/fallback）：

- **探针**：宿主注册 per-model 轻量验证（`drill(modelName, Callable<Boolean>)`
  或批量 `drillAll`）——便宜调用判可用（异常/false = 失败）。
- **记录**：成功记 lastVerifiedAt；失败记 lastFailedAt（不清 lastVerified——
  「上次验证成功」事实保留）；异常吞不上抛（演练不扰生产）。
- **新鲜度**：`isFresh(model, maxAge)` —— lastVerifiedAt 距今 < maxAge；
  `filter(candidates, maxAge)` 剔除过期未验者（保序）——降级链装配时只留
  「近期验证过」的备胎。
- 计数 drill-ok / drill-failed。

## User Stories

1. 作为运维，每 10 分钟演练一遍备链——凭证过期/配额没了在真降级前暴露。
2. 作为策略，降级链 filter 只留新鲜备胎——「存在」升级为「验证过」。
3. 作为宿主，演练成本可控（便宜探针 + 间隔归宿主）。

## Implementation Decisions

- Clock 注入；失败不驱逐（职责归 149——只标记未验证）。

## Testing Decimals

- 成功记新 isFresh true；失败记败且 lastVerified 保留；maxAge 过期 false；
  filter 保序剔除；异常吞计数；多模型隔离。

## Out of Scope

- 定时调度；自动切流；演练事件外发。

## Further Notes

- 备胎治理双面：驱逐（149，坏了的出局）+ 演练（本轮，好的持证上岗）。
