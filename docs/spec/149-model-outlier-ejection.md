# Spec 149 — 模型端点离群驱逐（effort #103）

> wayfinder map：`.wayfinder/maps/effort-103.md`（T505–T506）。借鉴：Envoy outlier
> detection——连续错误端点逐出集群，窗口过后试探复池。

## Problem Statement

备模型池有序（spec 64 快者优先）但成员资格不设防：一个持续 5xx 的坏端点会
反复被轮到（慢端点自然沉底，坏端点不一定慢）。Envoy 的解法是离群驱逐：
连续错误达阈值的端点暂时移出负载均衡池，窗口过后再给机会。

## Solution

`ModelOutlierEjection`（resilience/fallback）：

- **驱逐**：`recordError(model)` 连续达 consecutiveErrors（默认 5）→ 驱逐至
  ejectedUntil（now + ejectionWindow，默认 30s）；`recordSuccess` 复位连错。
- **复池**：窗口过期自动（下次 filter/isEjected 判定时）——无需显式解锁。
- **过滤**：`filter(List<NamedFallbackModel>)` 剔除在逐成员（保序）——挂在
  `FallbackChain.models()` 之后即成「健康池视图」。
- 观测：`ejectedModels()`（当前被逐名单）；计数 `buzhou.outlier.ejected`。

## User Stories

1. 作为宿主，坏端点被逐出备选池——降级链尝试的都是有资格的成员，坏端点不再
   白白消耗一次尝试与等待。
2. 作为运维，ejectedModels() 名单即「当下最可疑端点」——排障入口。
3. 作为端点，窗口过后自动复池试探——恢复即回归，无需人工干预。

## Implementation Decisions

- Clock 注入（窗口断言确定性）；per-model 单锁（对齐本仓状态机风格）。
- 只做连续错误面（延迟离群驱逐入档 Not yet specified——信号口径需先有 p99 基线）。

## Testing Decisions

- 连错逐出（isEjected 翻转）；窗口过复池；success 复位连错（不逐）；
  filter 剔除保序；models 互不影响；计数与参数校验。

## Out of Scope

- 延迟离群驱逐；驱逐比例上限；跨实例共享。

## Further Notes

- 池治理三件套：熔断（15）/ 延迟排序（64）/ 离群驱逐（本轮）。
