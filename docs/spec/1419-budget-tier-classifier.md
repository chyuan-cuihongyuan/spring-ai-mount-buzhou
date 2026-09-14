# 1419 — 预算分档分类器

> 来源：L 会话第 20 轮 = effort #1419（票 T2139 / T2140 / impl 1072）。借鉴：k8s ResourceQuota scope（软限预警先于硬限拒绝）+ Google SRE error budget headroom（80% 消耗即行动惯例）。

## Problem Statement

「已用/上限」散落各预算维度（token/请求数/会话数），看板与巡检需要把它翻译成**行动档位**。执行侧硬裁决（ModelBudgetGate）与读数（spec 1029 闸计数）、推荐（spec 806 分位档位）、多窗燃烧（spec 817）各有分工，但无状态离线分类面缺失——巡检报表/健康聚合无统一口径。

## 目标

- `BudgetTierClassifier`（core/budget，纯函数静态面，private 构造）：
  - `classify(Map<String,long[]> usageByBudget)`（名 → [used, limit]）→ `record TierReport(verdicts, greenCount, warnCount, hardCount, unknownCount)`；
  - 档位闭集 `enum Tier { GREEN, WARN, HARD, UNKNOWN }`：阈值常量 `WARN_RATIO=0.8` / `HARD_RATIO=1.0`（含端点——≥0.8 即 WARN、≥1.0 即 HARD）；
  - `BudgetTierVerdict(budget, used, limit, ratio, tier)`；`limit≤0` 畸形对判 UNKNOWN（ratio=-1，不冒充 GREEN）；
  - verdicts 饱和度降序平名典序（「先看谁快烧完」第一眼）+ 派生 `tightest(n)`（UNKNOWN 不参与）。
- 纯函数零状态：不触执行路径（预算闸语义不变），x 轴口径由调用方声明。

## 兼容性

纯函数零 IO 零状态；不触 ModelBudgetGate/预算闸计数。

## Out of Scope

- 告警/闸联动（分类器不裁决）。
- 预测性外推（806 分位推荐面）。
- 动态阈值配置面（常量显式，配置化另轮）。
