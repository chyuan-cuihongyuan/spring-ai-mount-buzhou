---
id: T856
title: GCRA 后端的算法映射与默认语义裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

令牌桶（InMemory 默认后端）容量即突发额度；对按平滑速率计 RPM 的供应商（突发即 429）需要无突发整形。GCRA（redis-cell / Envoy 同款）如何映射到既有 `RateLimitBackend` SPI？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 4 轮 = effort #600 / spec 603 / impl 456）：

1. **opt-in 新后端，默认不动**：`GcraRateLimitBackend` 经构造注入 `ModelRateLimiter`；默认仍是 InMemory 令牌桶（零行为变化）。
2. TAT 算法映射：τ = 60/容量；多单元（TPM amount>1）按同瞬连发 n cell（接受条件 `tat−now ≤ β+(n−1)τ`，接受后 `tat += n·τ`）；`consume` = 强推 TAT（对应令牌桶负余额的诚实超限）；预检（amount≤0）不推进 TAT。
3. 突发容忍 β 构造可配、**默认 0 = 严格平滑**（每 60/容量 秒放行一个——这正是与令牌桶的差异点，诚实入档）。
4. `available()` 语义定为「此刻还能连发几个」（floor((now+β−tat)/τ)+1，封顶容量）——与突发测试口径一致；未启用维度恒拒不抛（对齐 InMemory）。
5. nano 时钟可注入（测试确定性；生产 System::nanoTime 单调）。
