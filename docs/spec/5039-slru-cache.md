# Spec 5039 — Segmented LRU 分段缓存（effort #5039，S40）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6179–T6180，impl 2190）。
> 借鉴：PostgreSQL 缓冲区管理/Caffeine SLRU（双段晋升降级思想）。

## Problem Statement

缓存驱逐的病：普适 LRU 一次全表扫就把热数据全冲走
（扫描污染——命中率崩塌）——**分段晋升保护面**缺失。

## Solution

`SlruCache<K,V>`（core/cache）：

- 全容量切试用期/保护期两段（protectedRatio 定保护配额），
  段内各自 LRU；
- 新键入试用尾；命中一次即晋升保护尾；保护期满把保护头
  （最久未访问）**降级**回试用尾；淘汰只从试用头走——
  扫描键要冲垮保护段必须先在试用期被二次命中；
- `get` 晋升语义、`containsKey` 透视（不晋升）、upsert
  覆盖并晋升；
- 读数：size/probationarySize/protectedSize/evictedCount；
- fail-fast：capacity<1、ratio∉(0,1)、null 键值。

## User Stories

1. 作为缓存作者，一次性扫描不再污染保护段——热键存活。
2. 作为容量作者，双段读数——迁移与淘汰代价可见。

## Testing Decisions

- 满容淘汰试用头（a 出 e 进）；命中晋升+保护溢出降级
 （cap=4/prot=2：b,c 晋升→a 晋升降级 b；d 先于 b 出局）
 逐段读数钉住；扫描不污染（4 扫描键后 a,b 仍可取，
  evictedCount=4）；upsert 覆盖+晋升（v2 生效）；透视不
  晋升；畸形 fail-fast。

## Out of Scope

- 不做频率草图准入（TinyLFU 准入面后续轮覆盖）；不做
  TTL/过期（retention 面已有）；不做真实内存回收。

## Further Notes

- 与 ClockSweepCache（spec 5009）同族不同面：使用计数衰减
  vs 双段晋升降级。Wave 7 第四件。
- 里程碑：S40/50（80%）。
