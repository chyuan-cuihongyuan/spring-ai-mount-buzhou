# Wayfinder Map — Buzhou 虚拟 key 配额 Redis 共享（effort #315，C 会话第 16 轮）

> C 会话第 16 轮。VirtualKeys（148）是进程内计数——多实例部署时每实例各持
> 一份额度，N 实例 = N 倍烧钱（fog 152「key 配额 Redis 共享后端」项；
> 共享族先例：限流 54 / 熔断 57 / 舱 200 系）。

## Destination

core SPI `VirtualKeyBudgetBackend`（trySpend 原子语义跨实例）+ VirtualKeys
后端委托模式（无后端 = 既有进程内行为逐位不变）+ Redis 实现（Lua 原子
spend：spent+δ≤limit 才 INCRBY）+ store-redis 装配 bean。

## Notes

- 号段：spec 315 / T621–T622 / impl-338。
- 借鉴：Redisson 分布式信号量/限额器（原子扣减语义）。

## Decisions so far

- Lua 原子 spend（check-then-incr 竞态免 refund 复杂度）；键
  {@code <prefix>vk:<key>}，值 = 已用 tokens。
- 本地验证：jedismock 若不支持 EVAL 则测试 assumeTrue 跳过（真 Redis 语义
  归 CI Testcontainers——诚实边界）；core 侧用内存 fake 验委托语义。

## Out of scope

- key 注册表跨实例广播（register 仍进程内——限额声明本地、计数共享）；
- 余额回充（refund/赠额——预算语义单向）。

## Tickets

- [x] [T621 SPI + VirtualKeys 后端委托模式](tickets/T621-vk-backend.md)（impl-338）
- [x] [T622 Redis Lua 后端 + 装配 + 语义回归](tickets/T622-vk-redis.md)（impl-338）
