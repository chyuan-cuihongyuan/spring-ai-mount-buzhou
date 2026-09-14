# 1424 — 轮次限速 per-key 拒绝榜

> 来源：L 会话第 25 轮 = effort #1424（票 T2149 / T2150 / impl 1077）。借鉴：Cloudflare WAF top-rules（被限的是谁、拦了多少次——限流治理需要按 key 的拒绝分布而非全局计数）。

## Problem Statement

`TurnRateLimitHook`（轮次令牌桶限速，spec 138 同域）只有全局 `buzhou.ratelimit.turn-blocked` counter 与 availableSnapshot（可用令牌瞬时面）：**哪个 key 被拦了多少次**无读面——「租户 A 在反复触发限速」这种容量/公平问题只能从 Block 文案里逐条捞。

## 目标

- `TurnRateLimitHook`（core/ratelimit）增量：
  - per-key 拒绝计数表（256 封顶折 `__overflow__`——AgentBulkhead 拒绝表同纪律）；
  - `blockedSnapshot()`：key → 累计被拦次数（次数降序、同次数字典序——输出稳定）；
  - `resetBlockedForTest()`：清榜不清桶（桶状态/限速语义不动）。
- beforeTurn 拦截路径单点记账；放行路径零动作；Block 返回语义逐位不变。

## 兼容性

纯增量读面：限速判定/refill 算术/Block 文案逐位不变；256 封顶基数纪律防 key 基数失控。

## Out of Scope

- 按桶剩余令牌的分位（availableSnapshot 已有瞬时面）。
- 拒绝时刻台账（ToolSlowLog 式现场环——另轴）。
- 集群聚合（单进程口径）。
