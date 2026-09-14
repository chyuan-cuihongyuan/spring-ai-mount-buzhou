# 1602 · 熔断启动宽限期（K8s startupProbe 思想）

> 来源：N 会话 R3（effort #1602 / T2355–T2356 / impl 1155）。借鉴对象：Kubernetes
> startupProbe（>10K star 项目）——「startup 探针通过之前 liveness 不生效」：慢启动容器
> 不被存活探针误杀。同构映射：进程刚启动时的冷启动失败（建连、TLS 握手、DNS 预热抖动）
> 不该计入熔断开闸判定。

## Problem Statement

模型熔断器（spec 15）的失败窗口自进程启动即生效。发布/重启后的第一波请求带有天然的
冷启动失败（连接池未热、TLS 会话未建、供应商路由未稳），这些失败与真实故障在窗口里
无法区分——刚启动的实例可能在流量切入的头几秒被自己的熔断器跳闸，把「发布抖动」
放大成「发布即熔断」。K8s 用 startupProbe 解决完全同构的问题。

## Solution

`buzhou.resilience.circuit.warmup`（Duration，默认 0=关）：熔断器构造时刻起的一个宽限窗，
期内**跳闸判定豁免**——失败照常进窗口、成功照常冲淡率值、计数 `warmupSuppressedCount()`
记下每次豁免；宽限结束后已积累样本**立即恢复完整判定**（真故障会在宽限结束后的第一次
判定跳闸——宽限只放过启动抖动，不放过故障）。豁免只作用于本地跳闸判定；共享熔断闸
（spec 57）上其他实例已写的 OPEN 标记照常生效（他例故障不是本地启动抖动）。

## User Stories

1. 作为运维者，我想让发布后的冷启动失败不触发熔断，所以实例不会在流量切入头几秒被自己跳闸。
2. 作为运维者，我想让真故障在宽限结束后立即被跳，所以宽限只是延迟判定而非清空证据。
3. 作为运维者，我想保持默认行为零变化，所以不配 warmup 时跳闸语义与现状完全一致。
4. 作为运维者，我想看到启动抖动量，所以豁免次数有读数。

## Implementation Decisions

- `ResilienceProperties.Circuit` 扩参 `warmup`（第 10 参；既有 9/8/7/6 参兼容构造全保留，
  委托 null；负值 fail-fast）。
- `ModelCircuitBreaker` 构造时计算 `warmupUntil = clock.instant() + warmup`（0/null = null 关）；
  跳闸判定处（窗口率达标分支）先查宽限——期内 `warmupSuppressed++` 后 return（不开闸）。
- 进程级单时刻（非每模型）：进程启动时全部模型同处冷启动，单时刻语义直白。

## Testing Decisions

- `CircuitWarmupTest`（MutableClock 沿用 `CircuitTimeWindowTest` 模式）四断言：
  ① 宽限内灌满窗口不开闸 + suppressed 计数 + 宽限结束后下一次失败立即跳闸；
  ② 宽限内成功冲淡窗口 → 宽限结束后也不跳（自愈路径）；
  ③ 默认关：同序列立即跳闸（零变化钉死）；
  ④ 负 warmup 抛 `BuzhouConfigurationException`。
- Prior art：`CircuitTimeWindowTest`（时钟注入与状态断言）、`ModelCircuitBreakerTest`。

## Out of Scope

- 半开探测期的宽限（半开本身已是受控探测，无需再豁免）。
- per-model warmup（当前无按模型错峰启动的场景）。

## Further Notes

- 豁免与退避（spec 25）正交：宽限期内不跳闸 ⇒ consecutiveTrips 不增长 ⇒ 无退避放大。
