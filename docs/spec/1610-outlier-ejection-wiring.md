# 1610 · 离群驱逐生产接线 + 分类感知（spec 149 孤类救活）

> 来源：N 会话 R11（effort #1610 / T2371–T2372 / impl 1163）。

## Problem Statement

**重大发现**：ModelOutlierEjection（spec 149 / T505）在生产代码中零接线——
recordError/recordSuccess/filter 只有测试调用。机制存在、测试齐全、但从未接入
ResilienceAdvisor 调用路径——**等于关闭**。此外驱逐计数无分类过滤：AUTH（配置错误）
/CONTENT（内容治理）类失败也会驱动连错——与熔断的 failure-categories 语义不一致
（熔断明确这三类 IGNORED 不进窗口，因为跳闸/驱逐只该遮蔽可用性故障）。

## Solution

1. **生产接线**（opt-in `buzhou.resilience.outlier.enabled`，默认关零行为）：
   - 喂入：主模型/金丝雀目标/降级候选的成功（复位连错）与终态失败（连错 +1）——
     与 circuit.recordSuccess/recordTerminal 完全对称的挂点；
   - 过滤：两处降级候选迭代（金丝雀回退 + fallbackOrRethrow）从 `fallback.models()`
     改走 `outlier.filter(...)`（spec 149 的设计意图：「从降级链候选剔除在逐成员」；
     panic threshold 语义在 filter 内保留）；
   - 装配：进程级单实例（连错计数跨会话收敛——可用性是进程级事实），经
     ResilienceAssemblyCustomizer 注入每会话 advisor。
2. **分类感知**：`recordError(modelName, category)`——分类 ∈ `failureCategories`
   （默认 NETWORK/SERVER/TIMEOUT，可配，大小写归一）才计连错。旧单参 recordError
   委托 SERVER 计（存量语义等价：任何错误都计）。

## User Stories

1. 作为运维者，我想让坏端点被自动逐出降级链一个窗口，所以坏端点不再拖慢每次降级尝试。
2. 作为运维者，我想让配置错误不驱赶端点，所以 AUTH/CONTENT 类失败只修配置不逐端点。
3. 作为运维者，我想保持默认零行为，所以不配 outlier.enabled 时一切照旧。

## Implementation Decisions

- ResilienceProperties 顶层组件扩参 `Outlier`（16 参兼容构造保留）；
  Outlier 组：enabled/consecutive-errors/ejection-window/panic-threshold-percent/
  failure-categories，`toEjectionConfig()` 便捷转换。
- advisor 的 outlier 字段 volatile + `withOutlier` 链式注入（构造器链已深不再扩参）。

## Testing Decisions

- `ModelOutlierEjectionCategoryTest` 四断言：默认集过滤（AUTH/CONTENT/RATE_LIMIT
  不计、三次驱动类即逐、窗口过复池）/ 自定义集（NETWORK 被自定义集排除、大小写归一）/
  成功复位连错 / Outlier 组 → Config 转换。
- 回归：resilience 全量 377 用例（含既有 ejection/panic 测试）零变化。

## Out of Scope

- FallbackChainView.report 的 ejection 参数接线（观测端点装配——后续健康面轮）。
- 驱逐事件流（circuit.state-changed 同款事件面——喂入已有指标 counter）。

## Further Notes

- 孤类发现的流程教训：机制建成 ≠ 机制生效——本轮起「原语类必须 grep 生产调用面」
  进自查清单。
