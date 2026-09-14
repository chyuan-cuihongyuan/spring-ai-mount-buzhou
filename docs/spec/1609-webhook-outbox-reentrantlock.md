# 1609 · WebhookOutbox 锁迁移（spec 1606 排队项）

> 来源：N 会话 R10（effort #1609 / T2369–T2370 / impl 1162）。spec 1606 审计中危 #1
> 落地：outbox 四方法的 monitor 内含 store put/scanByPrefix 多次往返——WebhookEventForwarder
> 的 dispatchLoop 运行在虚拟线程（放大因素），生产接 JDBC/Redis store 即锁内网络 IO。

## Solution

`append` / `appendRetry` / `orphanIndexCount` / `requeueDead` 四个包级 synchronized
方法 → ReentrantLock wrapper + `*Locked` 私有方法体（原逻辑零动）。互斥语义不变
（outbox 的入队/重试/审计/死信迁移互斥保留）；阻塞等锁的虚拟线程 unmount 不 pin。

## Testing Decisions

- 回归：webhook 包全量（105 用例）零变化——outbox 互斥语义已被既有行为测试覆盖
  （入队容量、退避、孤儿审计、死信迁移等），无需新增并发用例。

## Out of Scope

- store-in-lock 家族其余成员（TokenBudgetHook/SessionQuotaHook/PeriodBudgetHook/
  EpisodeLedger——每模型响应触发的 hook 路径，下一批排队）。
