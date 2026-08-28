# Spec 79 — outbox due-time 索引（effort #40）

> wayfinder map：`.wayfinder40/MAP.md`（T311–T312）。#35 fog「outbox due-time 键序
> 结构」收口；spec 78（scanByKeyRange）的消费方。借鉴：Kafka log+index / LSM
> base+index 双结构（自愈容错双写竞窗）。

## Problem Statement

`WebhookOutbox.due()` 每轮 scanByPrefix 全量读 outbox.* 值并解析：退避积压越大
（长退避记录越多），每轮无效读放大越大——spec 58 消了容量计数的读放大，调度路径
的读放大仍在。

## Solution

due-time 索引键 `due.<16 位零垫 nextAttemptAtEpochMs>.<eventId>` 与记录双写：
append/requeue 建键、update 迁键（旧键删新键建）、delete/markDead 清键；`due()`
走 `scanByKeyRange("due.", null, "due."+pad(now+1), limit)` 取最早到期者。孤儿
（记录已删）与陈旧（记录已后移）索引在 due() 读路径就地清键自愈；构造期对存量
记录幂等回填（旧版数据迁移）。损坏记录仍走既有隔离死信路径。

## User Stories

1. 作为运维，我要退避积压不再放大调度读，所以投递延迟不随积压线性恶化。
2. 作为宿主，我要旧版数据零手工迁移，所以升级即回填。
3. 作为红队，我要崩溃竞窗（双写只落一半）自愈，所以无需分布式事务。

## Implementation Decisions

- limit 按到期序取（旧按 seq）——重退避者不被挤饿；跨实例 seq 本无序保证（spec 24）。
- 索引 value = eventId（最小可回查面）；索引键不占容量计数（outbox. 前缀语义不变）。
- 内部 API 语义升级：delete/update 携带记录（旧 due 键删除依据）——包私有面，
  调用方（forwarder）同步。

## Testing Decisions

- 退避迁键：未来不可见、迁回恢复；最早优先 limit=1；
- 孤儿 + 陈旧索引自愈清键；生命周期出口（delete/markDead/requeue）无残留；
- 旧格式直铺 store → 构造期回填后照常可投。

## Out of Scope

- Redis 有序结构覆写；fsck due 对账项（fog 记账）；索引 TTL。

## Further Notes

- 键序即时间序是「时间编进键」的通用模式——后续冷层归档/清理调度可复用
  （spec 78/79 组合）。
