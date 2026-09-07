# Spec 163 — 配置热重载原语（effort #110）

> wayfinder map：`.wayfinder/maps/effort-110.md`（T521–T522）。借鉴：Caddy config
> reload（原子换引用 + 变更广播）/ Spring Cloud refresh。

## Problem Statement

运行参数（限幅阈值/预算上限/超时档位）在装配期定死后只能重启更新——夜间
调参窗口贵；而「换引用 + 通知」这个原语每家手搓一遍还容易搓错（可见性/
版本/监听泄漏）。

## Solution

`ReloadableConfig<T>`（core/config）：

- `of(initial)` 构造（initial 非空 fail-fast）；
- `current()`——volatile 读（读侧零锁）；
- `replace(next)`——原子替换 + 版本自增 + 通知全部订阅者（同值也计版本——
  重放安全，观察方按版本幂等）；
- `updateIf(Function<T,T>)`——条件 CAS 换（读-改-写窗口内他方已换则重试）；
- `subscribe(Consumer<T>)` → 退订句柄（防监听泄漏）；
- `version()` 单调版本号。

## User Stories

1. 作为宿主，我的调参接口 replace 一发即热生效——读侧 current() 每次拿最新。
2. 作为策略，updateIf 按「当前值」条件更新——并发调参不丢条件。
3. 作为订阅方（如重建缓存），版本号让我幂等消费变更流。

## Implementation Decisions

- volatile 引用 + synchronized 写（写低频读高频——读写不对称适配）。
- 订阅者 CopyOnWriteArrayList；通知在锁外（防死锁）。

## Testing Decisions

- 初值/current；replace 生效+版本+通知；退订后不再收；updateIf 条件换与
  并发重试语义；null 初值/替换 fail-fast；多订阅者全收。

## Out of Scope

- watcher 实现；配置中心客户端；diff。

## Further Notes

- 热更面拼图：本原语（本轮）+ 具体参数接线（各机制后续按需）。
