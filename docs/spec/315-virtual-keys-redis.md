# Spec 315 — 虚拟 key 配额 Redis 共享（effort #315）

> wayfinder map：`.wayfinder315/MAP.md`（T621–T622）。借鉴：Redisson 分布式
> 限额器（fog 152「key 配额 Redis 共享后端」项；共享族先例 54/57/200 系）。

## Problem Statement

VirtualKeys 配额计数进程内自持：多实例部署每实例各一份额度——N 实例 = N 倍
烧钱，key 级成本硬顶形同虚设。

## Solution

**core SPI**（`VirtualKeyBudgetBackend`）：

- `trySpend(key, tokens, limitTokens)` 原子语义（spent+δ ≤ limit 才提交）；
  `usedTokens(key)` 点查；`reset(key)` 清零。
- `VirtualKeys.withBackend(backend)` 委托模式：register 仍进程内（限额声明
  本地——MAX_KEYS 封顶照旧）；trySpend/usage/reset 走后端。无后端 = 既有
  进程内行为逐位不变。

**Redis 实现**（store-redis，`RedisVirtualKeyBudgetBackend`）：

- Lua 原子 spend（check-then-incr 一体——竞态窗口零）；键
  {@code <prefix>vk:<key>} 值 = 已用 tokens；DEL 清零。
- 装配：store.type=redis 时供 bean（与共享限流/熔断后端同装配面）。

## User Stories

1. 作为运维，4 实例共享一份 key 额度——烧钱硬顶是真顶。
2. 作为宿主，单实例部署无感（默认无后端零变化）。

## Implementation Decisions

- 不做余额回充（预算单向）；异常态（后端不可达）trySpend 返回 false（fail
  closed——配额面宁可拒绝不可超支）。

## Testing Decisions

- core `VirtualKeysBackendTest`（内存 fake 后端）：委托语义（spend 走后端/
  usage 读后端/reset 透传）+ fail-closed + 无后端回归。
- store-redis `RedisVirtualKeyBudgetBackendTest`（jedismock；EVAL 不支持则
  assumeTrue 跳——真 Redis 语义归 CI）：限额边界拒绝/清零重置/跨实例计数
  合流（两个 backend 实例同键）。

## Out of Scope

- 注册表跨实例广播；余额回充。

## Further Notes

- 共享族进度：限流（54）/ 熔断（57）/ 语义缓存（125）/ 舱（200 系）/
  **key 配额（本轮）**——泳道共享归下轮。
