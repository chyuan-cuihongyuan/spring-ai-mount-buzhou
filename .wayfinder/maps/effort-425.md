# Wayfinder Map — Buzhou 轮次租户限速（effort #425，D 会话第 26 轮）

> D 会话第 26 轮（原 30 主题池「租户限速(nginx)」）。勘察：限流面已有
> ModelRateLimiter（resilience，per-model RPM/TPM）与 SpawnGate（并发
> 上限）——**时间维速率**（每分钟 N 次）在轮次/租户维空白：一个失控
> 会话/租户可以不限频次地打满模型（预算只管花钱多少不管来得多人）。
> nginx token bucket（突发桶+匀速回填）语义缺位。

## Destination

`core.ratelimit.TurnRateLimitHook`（beforeTurn 准入，ORDER=210 早于
守卫族 220+）：

- 惰性令牌桶 per key：`burst` 桶容 + `permitsPerMinute` 匀速回填
  （nanoSupplier 注入可测）；每次 turn 扣 1；
- key 默认 sessionId（per-session 频次帽）；可插拔 `keyFunction`
  （宿主给常量键=per-runtime/租户整体帽——TokenBudgetHook 构造身份
  同法）；
- 超限 `HookResult.block`（守卫族同词汇）+ `buzhou.ratelimit.turn-
  blocked` 计数（无 tag——session 键无界纪律）；
- `availableSnapshot()` 观测面（各 key 余量）；
- yml：`buzhou.ratelimit.turns.{burst, permits-per-minute}` 双声明即
  装配（默认键 sessionId）。

## Notes

- 号段：spec 425 / T741–T742 / impl-398。
- 借鉴源：nginx rate limiting（burst+rate 令牌桶）+ Lazy token bucket
  惰性回填（无定时器）。
- 纪律：桶 map 无界=键受会话数上界（诚实边界注记）；Block 不炸轮
  （拒绝即响应——守卫族语义）。

## Out of scope

- 跨实例共享限流（Redis 域——已否决池）；排队等待（SpawnGate 排队
  已有，速率域拒绝优先信号质量）；per-key 差异化配额（单 Policy 面）。

## Tickets

- [x] [T741 TurnRateLimitHook 令牌桶](../tickets/T741-turn-rate-limit-hook.md)
- [T742 yml 装配+时钟语义用例](../tickets/T742-turn-rate-limit-assembly.md)
