# Wayfinder Map — Buzhou 延迟感知备模型排序（effort #24）

> effort #24（已闭合 2026-08-29），延续 #5–#23；收口后累计 171 轮 / impl 1–210。
> 主线：**延迟感知备模型排序**——降级链按静态配置序尝试；LiteLLM Router
> latency-based routing（按平均延迟选 deployment）思想：按 EMA 延迟排序候选，
> 快者优先，慢者少打。

## Destination

`buzhou.resilience.fallback.latency-aware=true`（默认关）时：备模型调用延迟入
EMA 追踪（α=0.3），降级遍历序按 EMA 升序（未知延迟取已知中位数 = 中性不动原序）；
金丝雀/限流/熔断语义不变；新键 1 个登记矩阵与 metadata；默认关零行为变化。

## Notes

- 外部事实源：LiteLLM latency-based routing（lowest-avg-latency 优先 + 无数据冷启动）。
- 本地裁定：EMA 而非滑动均值窗（O(1) 无窗内存）；未知 = 已知中位数（新模型不被
  饿死也不插队——诚实中性）。
- Fallback record 4→5 组件：兼容构造保留（semantic-cache 同先例，api-surface 记破坏性）。

## Decisions so far

- 追踪点 = advisor 三处备模型/金丝雀调用的 timed 包装（模型级纯延迟，不含 advisor 链）。
- 排序点 = FallbackChain.models()（tracker 非 null 时返回稳定排序视图；原序并列）。

## Not yet specified

- 延迟分位（P95）排序；按 token 吞吐归一（LiteLLM TPM-aware）——量级证据后议。

## Out of scope

- 沿用 #7–#23；主模型路由选择（fallback 之外）；新存储。

## Tickets

- [x] [T279 FallbackLatencyTracker + FallbackChain 排序 + advisor 计时接线 + 新键](../tickets/T279-latency.md)（impl-210）
- [x] [T280 红队（EMA 序/未知中性/稳定性/E2E 切换计时）+ 矩阵登记 + verify + 收口](../tickets/T280-latency-close.md)（impl-210；7 例 + 矩阵绿；累计 171 轮）
