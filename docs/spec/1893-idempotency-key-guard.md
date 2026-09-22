# Spec 1893 — 幂等键判定面（effort #1893，R94）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2987–T2988，impl 1494）。借鉴：
> Stripe（万星级 API 惯例）idempotency-key 三态语义——同键同参重放、
> 同键异参拒绝（idempotency_error）、TTL 过期失效。与 501 幂等
> Advisor（重放存储面）互补：这是键判定的纯决策面。

## Problem Statement

幂等键只判「键存在与否」会漏最危险的场景：客户端复用旧键但改了
请求参数——重放旧响应等于把 A 请求的结果给 B，副作用错配。键的
参数指纹冲突与 TTL 失效缺独立判定面。

## Solution

`IdempotencyKeyGuard`（core/transaction，静态纯函数 + 嵌套
Decision 枚举）：

- `decide(storedFingerprint, incomingFingerprint)`：存档缺席 →
  FIRST（放行执行）；指纹相等 → REPLAY（重放缓存响应）；不等 →
  CONFLICT（同键异参，拒绝）；
- `isExpired(createdAtMillis, nowMillis, ttlMillis)`：now ≥ created+ttl
  → 键失效（可清档案重执行）。

## User Stories

1. 作为幂等实现者，三态判定一函数——存储面只需按 Decision 行事。
2. 作为 API 设计者，CONFLICT 即 Stripe idempotency_error 语义——
  「改参数复用键」被物理拦住。
3. 作为清理者，TTL 过期读数——24 小时后键可安全回收。

## Implementation Decisions

- 纯函数零状态；null 存档 = FIRST 口径（首见）；ttl ≥ 0、时点 ≥ 0
  fail-fast；边界恰到期即失效。

## Testing Decisions

- 三态各一例（FIRST/REPLAY/CONFLICT）；TTL 边界两例（恰到期失效/
  差一毫秒存活）；畸形两型（负 TTL/负时点）fail-fast。

## Out of Scope

- 不做键存储与响应缓存（归 501 Advisor/ResponseCacheStore 面）；
  不做指纹算法约定（哈希族已有）。

## Further Notes

- 与 IdempotencyAdvisor（#501）互补：那是存储与重放管线，这是
  决策语义；与幂等冲突读面（L-1700）互补：那是冲突计数，这是判定。
