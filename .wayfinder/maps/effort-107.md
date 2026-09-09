# Wayfinder Map — Buzhou 弹性预算池（effort #107，B 会话第 19 轮）

> B 会话第 19 轮。主题池「预算再分配」：会话预算是静态分配——闲会话的配额
> 沉睡，忙会话只能撞墙。借鉴 Spark AQE（运行时把空闲资源再分配给忙分区）。

## Destination

ElasticBudgetPool（core/budget）：容量 C + 各会话基础配额（Σbase ≤ C 保底）；
超额借用只吃 surplus（C − Σmax(base, held)）——基础配额永不被借走；
release 归还 surplus。计数 borrowed/denied。

## Notes

- 号段：B=奇数 spec（本轮 157）。
- 与 spec 16 预算闸（会话内硬顶）正交：那是单会话天花板，这是池级再分配层。

## Decisions so far

- 基础保底用 max(base, held) 口径——借走的东西不召回（在飞借用不中途斩）。

## Not yet specified

- 空闲回收（idle 会话 base 缩容）；按优先级借用。

## Out of scope

- 沿用 #7–#106；跨实例全局池（Redis 化）；异步等待配额。

## Tickets

- [x] [T515 ElasticBudgetPool（保底+借用+归还）](../tickets/T515-budget-pool.md)（impl-291）
- [x] [T516 池回归（保底永在/借 surplus/护 base/归还）](../tickets/T516-budget-pool-tests.md)（impl-291）
