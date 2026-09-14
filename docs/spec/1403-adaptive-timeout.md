# 1403 — EWMA 自适应超时推荐器

> 来源：L 会话第 4 轮 = effort #1403（票 T2107 / T2108 / impl 1056）。**换题记录**：原题「重试预算」勘察发现 RetryBudget/RetryBudgetHolder 已存在（spec 178 §A，Finagle retry budget 全撞）——换入备选方向的 EWMA 自适应超时轴（该轴零命中开放）。借鉴：Envoy timeout budget / Finagle 自适应超时（超时从实测时延推导而非拍脑袋定值）。

## Problem Statement

固定模型/工具超时两难：太松则故障探测迟钝（挂死调用占满预算窗口），太紧则正常慢请求被误杀。时延分布随 provider/时段漂移——静态配置无从跟随。仓内 RetryBudget（重试风暴）与 AdaptiveBulkhead（并发上限自适应）皆有，**超时维度无自适应推导器**。

## 目标

- `AdaptiveTimeout`（resilience 根包，纯推导器）：`record(observedMillis)` 喂样本，`recommended()` 出推荐。
  - EWMA：`e′ = α·x + (1−α)·e`（首样本播种；α=0.3 默认——新样本权重 30% 兼顾响应与抗噪）；
  - 推荐 = `clamp(⌈EWMA×multiplier⌉, floor, ceiling)`（默认 3×、[50ms, 60s]；乘数覆盖长尾）；
  - 预热哨兵：样本 < `MIN_SAMPLES`(3) 返回 `Optional.empty()` 不下结论（BudgetRecommendation 先例）；
  - CAS 参与式无锁更新；`stats()` 无副作用快照（不推进推荐计数）+ `resetForTest()`。
- **不接线执行路径**：超时执行归调用方既有机制（turnBudget/HookAdvisor 族），接线留装配轮。

## 兼容性

纯函数类，零静态状态，不触任何执行路径/配置面；参数非法 fail-fast（α∉(0,1]、乘数<1、floor>ceiling、负样本）。

## Out of Scope

- 分位数（P95/P99）推导——EWMA 均值语义 + 乘数覆盖长尾已满足推荐诉求；分位面归 TurnLatencyPercentiles 族。
- per-provider/per-tool 实例编排（多实例由调用方自建）。
- 超时执行接线（装配轮）。
