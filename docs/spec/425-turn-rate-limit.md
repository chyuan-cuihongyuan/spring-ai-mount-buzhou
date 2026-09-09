# Spec 425 — 轮次租户限速（effort #425）

> wayfinder map：`.wayfinder/maps/effort-425.md`（T741–T742）。D 会话第 26 轮。

## Problem Statement

限流面已有 per-model RPM/TPM（ModelRateLimiter）与并发上限
（SpawnGate）——时间维速率（每分钟次数）在轮次/租户维空白：失控
会话可不限频次打满模型（预算管花钱多少，不管来得多人）。

## Solution

`core.ratelimit.TurnRateLimitHook`（nginx token bucket 借鉴）：

- **令牌桶**（惰性回填——无定时器，nanoSupplier 注入可测）：per key
  `burst` 桶容 + `permitsPerMinute` 匀速回填，每次 turn 扣 1。
- **key 策略**：默认 `sessionId`（per-session 频次帽）；构造可插拔
  `keyFunction`（常量键 = per-runtime/租户整体帽——TokenBudgetHook
  构造身份同法）。
- **超限语义**：`HookResult.block`（守卫族同词汇——拒绝即响应不炸轮）
  + `buzhou.ratelimit.turn-blocked` 计数（无 tag——session 键无界纪律）。
- **观测**：`availableSnapshot()` 各 key 余量快照。
- yml：`buzhou.ratelimit.turns.{burst, permits-per-minute}` 双声明即
  装配（默认 sessionId 键）。

## User Stories

1. 作为服务作者，我想单会话每分钟至多 N 轮， so 失控循环不把模型配额
   打满（预算之外多了频次维度）。
2. 作为多租户宿主，我想按租户整体限速， so 单租户不挤占其他租户的
   吞吐。

## Implementation Decisions

- ORDER=210（早于守卫族 220+——速率准入最外层先判）。
- 桶 map 按 key 惰性建（键空间=会话数上界，诚实边界注记）。
- Bucket synchronized（单桶争用可接受——回填算术两行）。

## Testing Decisions

- burst=2/1 每分钟：首 2 轮过、第 3 轮 block；时钟推进 60s 回填后放行；
  部分回填按比例（30s=0.5 令牌）。
- 双 key 独立（s1 打满不碍 s2）；常量键共享（两会话同桶）。
- block 后余量不减；availableSnapshot 反映扣减。
- yml：双声明出 bean、缺一不出。

## Out of Scope

- 跨实例共享（Redis 域）；排队等待；per-key 差异化配额。

## Further Notes

- 新公共类型 `TurnRateLimitHook`、`BuzhouTurnRateLimitProperties` 随轮
  regenerate 快照。
