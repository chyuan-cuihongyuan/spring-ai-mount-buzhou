# Wayfinder Map — Buzhou 共享事实库 ACL（effort #410，D 会话第 11 轮）

> D 会话第 11 轮。勘察（2026-09-08）：FactStore 是**会话内**事实
> （save(sessionId, fact)/activeFacts(sessionId, turn)）——跨会话/跨 agent
> 共享事实（团队级知识、全局偏好）无承载；更无 ACL（谁能读谁的事实）。
> mem0 的共享记忆 + 按用户隔离思想无对应物。

## Destination

`core.fact` 包（mem0 借鉴——共享记忆 + 隔离）：`SharedFact`（key/value/
owner/createdAt/可选 ttl）+ `SharedFactStore` 接口（publish/grant/revoke/
read/readable）+ `InMemorySharedFactStore`（**deny-by-default**——owner 恒
读、显式 grant 才可读、revoke 幂等；非 owner 发布已存在键 fail-fast——
键即所有权；ttl 过期读不到；拒绝读计数 `buzhou.facts.denied-reads`）。
bean 恒在（空库零行为；宿主程序面注入使用——API 轮）。

## Notes

- 号段：spec 410 / T711–T712 / impl-383。
- 借鉴源：mem0（30k★）共享记忆 + 按用户隔离；deny-by-default 是最小
  安全默认。
- 纪律：值 Object（序列化归宿主——store 不假定 JSON）；ttl 时钟可注入
  （测试确定性）；无事件流（库语义——观察扩散候选）。

## Out of scope

- 持久化后端（JDBC/Redis 扩散候选）；跨实例共享；读者通配/组语义
  （真需求再议）；审计流（audit 族扩散候选）；yml 播种。

## Tickets

- [x] [T711 SharedFactStore ACL 语义](../tickets/T711-shared-fact-store.md)
- [x] [T712 InMemory 实现 + bean](../tickets/T712-shared-fact-inmemory.md)
