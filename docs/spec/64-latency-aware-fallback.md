# Spec 64 — 延迟感知备模型排序（effort #24）

> wayfinder map：`.wayfinder/maps/effort-24.md`（T279–T280）。OSS 借鉴：LiteLLM Router
> latency-based routing。

## Problem Statement

降级链按静态配置序尝试备模型：配置序与实际延迟表现脱节——链首模型若持续慢（未慢到
超时），每次降级都先打它再等它失败/超时才轮到快模型；延迟信息已经在每次调用中产生，
却没有反馈进候选顺序。

## Solution

opt-in（`buzhou.resilience.fallback.latency-aware`，默认 false）：备模型与金丝雀目标的
调用延迟入 EMA 追踪（α=0.3，O(1)）；降级遍历序按 EMA 升序——未知延迟取已知 EMA 的
中位数（新模型中性不动原序，不插队不饿死）；并列保原序（稳定排序）。

## User Stories

1. 作为运维者，我要降级先打实测快的备模型，所以降级总延迟不被链首慢模型拖累。
2. 作为运维者，我要 EMA 反映近期表现（α=0.3），所以慢化趋势会在几次调用内传导到排序。
3. 作为红队，我要无数据的模型保持原序（中性），所以新上链模型不被饿死也不冒进。
4. 作为红队，我要默认关零行为变化，所以升级零风险。
5. 作为 API 治理者，我要新键登记矩阵，所以配置防线不静默漂移。

## Implementation Decisions

- 追踪点：advisor 内备模型/金丝雀三处调用 timed 包装（模型级纯延迟）。
- 排序点：FallbackChain.models() 返回稳定排序视图（tracker 非 null）。
- Fallback record 4→5 组件（latencyAware）：4 参兼容构造保留（源码兼容；反射绑定
  按 canonical 的调用方需核对——与 semantic-cache 同先例入档）。

## Testing Decisions

- 单测：EMA 更新、中位数中性、稳定并列、models() 排序视图；E2E：主模型失败降级时
  tracker 记录备模型延迟（断言 tracker 状态）。

## Out of Scope

- 分位数排序；TPM 归一；主模型选择路由；新存储。

## Further Notes

- 金丝雀/限流/熔断跳过语义不变（排序只改「先试谁」）。
