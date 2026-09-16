# Spec 1875 — 级联失败暴露读面（effort #1875，R76）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2951–T2952，impl 1476）。借鉴：
> 级联失败分析惯例（Hystrix 舱壁思想源头）——依赖边权重 = 流量占比 ×
> 下游失败率，高权重边即级联风险边。

## Problem Statement`

熔断器逐点防护（单下游倒下掐断自己），但**边拓扑上哪里最脆**无人答：
主流量 80% 压在失败率 50% 的下游——这条边的期望损失面 0.4 是全图之最，
熔断器却只在它倒下后才动作（事后）。脆边排序与活/静风险分诊缺面。

## Solution

`CascadeExposure`（buzhou-resilience，静态纯函数）：

- `Edge(from, to, trafficShare, downstreamFailureRate)` 契约（两率
  ∈ [0,1]）——`riskWeight()` = share × failureRate；
- `analyze(edges, health)` → `Exposure(edges, activeRiskyEdges, worstEdge,
  worstWeight, totalWeight)`：下游不健康且权重 >0 即活风险（正在传导），
  健康 即静风险（埋着）；并列最重取首（入参序）；
- `activeRatio()` 活风险占比（无边 -1 哨兵）。

## User Stories

1. 作为容量架构者，worstEdge=api->db（权重 0.4）→ 该分流量或加兜底
   ——事前减脆不靠事后熔断。
2. 作为值班者，activeRatio>0 → 有边正在传导，先掐活风险再排静风险。
3. 作为框架宿主，节点与流量口径自声明，纯读不熔断。

## Implementation Decisions

- 纯读不熔断（动作归 ModelCircuitBreaker 族）；下游健康缺席按健康
  （不臆断故障）。

## Testing Decisions

- 权重排序+最脆边；活/静风险分诊；空表哨兵+并列取首；畸形四型
  fail-fast。

## Out of Scope

- 不做多跳传导模拟（图遍历归未来静脉）；不执行熔断。

## Further Notes

- 与 ModelCircuitBreaker 互补：那是逐点防护执行，这是拓扑脆性读面。
