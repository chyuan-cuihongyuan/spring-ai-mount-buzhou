# Spec 410 — 共享事实库 ACL（effort #410）

> wayfinder map：`.wayfinder/maps/effort-410.md`（T711–T712）。D 会话第 11 轮。

## Problem Statement

FactStore 是会话内事实——跨会话/跨 agent 共享知识（团队级偏好、全局
事实）无承载；「谁能读谁的事实」的 ACL 无从谈起。会话 A 学到的「用户
在沪」会话 B 无法受益，也无法约束 B 不该读 A 的私域事实。

## Solution

`core.fact` 包（mem0 共享记忆 + 隔离借鉴）：

- **`SharedFact`**（record）：key/value/owner/createdAt/可选 ttl（Duration，
  null = 永久；读时按 Clock 判过期）。
- **`SharedFactStore`**（接口）：
  - `publish(SharedFact)`：发布/覆盖自己的键；**非 owner 发布已存在键
    fail-fast**（键即所有权——抢键是配置错误不是竞争）；
  - `grant(factKey, reader)` / `revoke(factKey, reader)`：显式授权/收回
    （revoke 幂等；owner 恒读——revoke owner 无效）；
  - `read(reader, factKey)`：owner 或被授权者可得值；否则 empty
    （**deny-by-default**）+ 拒绝计数 `buzhou.facts.denied-reads`；
  - `readable(reader)`：该 reader 可读的全部事实（owner 键 + 被授权键，
    过期滤除）。
- **`InMemorySharedFactStore`**：ConcurrentHashMap 实现；Clock 可注入。
- 装配：bean 恒在（空库零行为——宿主程序面注入使用）。

## User Stories

1. 作为 agent 作者，我想会话 A 学到的事实显式共享给会话 B，so 团队
   级知识不困在单会话。
2. 作为安全负责人，我想默认只有 owner 能读，so 私域事实不被旁路
   会话窥探。
3. 作为运维，我想拒绝读有计数，so 探测行为（谁在试探读没被授权的
   键）可见。

## Implementation Decisions

- 值 Object（序列化归宿主——store 不假定 JSON）。
- 无事件流（库语义——观察扩散候选）。

## Testing Decisions

- owner 恒读 + 覆盖自己的键；grant 后可读/revoke 后拒绝/幂等；
- 非授权 empty + 计数；非 owner 发布已存在键 fail-fast；
- ttl 过期（Clock 拨动）；readable 集合正确（owner+granted、过期滤除）。

## Out of Scope

- 持久化后端；跨实例；组/通配读者；审计流；yml 播种。

## Further Notes

- 新公共类型 `SharedFact` / `SharedFactStore` / `InMemorySharedFactStore`
  随轮 regenerate 快照。
