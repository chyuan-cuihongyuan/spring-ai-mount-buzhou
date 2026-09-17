# Spec 3022 — 二择选择器（effort #3022，R23）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5045–T5046，impl 2023）。
> 借鉴：Power of Two Choices（Azar 1994 / Mitzenmacher）。

## Problem Statement

多目标派单（工具端点/模型上游/工作者）朴素随机单抽的最大负载
Θ(ln n/ln ln n)——尾部桶显著过热；全局最小负载扫描又要中心表
与 O(n) 决策成本。

## Solution

`TwoChoiceSelector`（core/concurrent，纯函数静态件）：

- `pick(loads, rng)`：独立抽两相异下标，取负载小者（并列取先抽
  ——等负载退化为均匀随机）；
- **O(1) 决策换 Θ(ln ln n) 级最大负载**（两问取轻的指数级改善）；
- loads 快照由调用方维护（派单自增回写——本件零状态）；负载
  非负、数组非空校验；RandomGenerator 注入回放。

## User Stories

1. 作为派单作者，不扫全表不加中心队列，两问取轻——尾部桶过热
   指数级缓解。
2. 作为对账作者，纯函数零状态——派单策略可回放可对拍。

## Testing Decisions

- 唯一空桶选中率 15/16≈94%（±界）；万球十桶二择最大负载 ≤ 单抽
  同种子族且 ≤1020（Θ(ln ln n) 紧界证据）；等负载均匀 ±20%；
  单桶恒 0；同种子回放；空数组/负负载 fail-fast。

## Out of Scope

- 不做负载表维护（快照归调用方）；不做加权二择（按容量缩放
  留白）；不做 n 抽（d-choices 泛化留白）。

## Further Notes

- 与 WeightedRouter（权重路由）/ JumpConsistentHash（分片路由）
  正交：路由定归属，二择定派单——可叠加。
- 里程碑：23/150。
