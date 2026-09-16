# Spec 2044 — 预热斜坡（effort #2044，R45）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3189–T3190，impl 1595）。
> 借鉴：Guava warmup limiter——冷启动速率渐升，防首秒洪峰。

## Problem Statement

新下游（端点/连接池/模型路由）上线即满速：冷缓存/冷连接池首秒洪峰
把刚起的下游打挂——需要「从起始比例平滑爬升到满速」的预热期乘数。

## Solution

`WarmupRamp`（core/backpressure，不可变纯函数——时刻外注入）：

- `factorAt(elapsed)` ∈ [startFactor, 1]：预热内（0, warmup) 线性爬
  升（0 恰 startFactor、warmup 恰 1.0）；期满恒 1.0；回拨宽进按起点；
- `warmedUp(elapsed)` 期满判定；startFactor()/warmupMillis() 回显；
- 契约：warmup > 0、startFactor ∈ (0,1) fail-fast；默认 30s/10%。

## User Stories

1. 作为发布作者，新路由 30s 从 10% 爬满——下游冷启动不被首秒洪峰
   打挂。
2. 作为调用方，乘数作用在自身限流器/并发闸——与 BurstCredit（透支）
   正交（渐升 vs 突发）。

## Testing Decisions

- 起点 0.1 恰；中点 0.55/1/4 点 0.325 线性；恰期满 1.0 + 恒满 +
  warmedUp 边界（9999/10000）；200 步单调不减；回拨宽进；构造回显；
  畸形三型 fail-fast。

## Out of Scope

- 不做平滑曲线（线性口径）；不接限流器装配（乘数消费归调用方）。

## Further Notes

- 与突发信用（spec 1872）成对：透支管「快」，预热管「慢下来起步」。
