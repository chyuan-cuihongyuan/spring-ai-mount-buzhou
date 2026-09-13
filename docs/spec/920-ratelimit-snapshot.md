# 920 — webhook 限流器余量快照读面

> 来源：I 会话第 21 轮 = effort #920（[T1291](../../.wayfinder/tickets/T1291-ratelimit-snapshot-shape.md) / [T1292](../../.wayfinder/tickets/T1292-ratelimit-snapshot-verify.md) / impl 673）。TurnRateLimitHook.availableSnapshot 先例同构（spec 13 §core-3 读面纪律在限流域补齐）。

## Problem Statement

`WebhookRateLimiter` 只有 tryAcquire 判定与 deferredCount——令牌余量、桶容量、流速配置不可读。运维无法区分「限流配置过低（余量常态贴 0）」与「突发超预期（余量骤降后回填）」，deferred 高企时缺根因线索。

## 目标

- `WebhookRateLimiter.snapshot()`（synchronized 同锁强一致）：
  - `record Snapshot(double tokens, double capacity, double refillPerSecond, long deferred)`；
  - tokens 为 refill 时点修正后的实时余量（复用既有 refill 私有逻辑——修正语义与 acquire 判定一致）；
- 纯读面：tryAcquire / deferredCount 既有行为零变化。

## 兼容性

纯增量：公共类新增方法 + 公共嵌套 record，零既有行为变化。
