# Spec 5009 — Clock-Sweep 缓存驱逐（effort #5009，S10）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6119–T6120，impl 2160）。
> 借鉴：PostgreSQL Buffer Manager clock-sweep（使用计数 + 环形指针）。

## Problem Statement

缓存驱逐的病：纯 LRU（偶发扫描把热页洗出去——一次全表扫描
污染整缓存）或 LFU 全量计数（老页计数奇高永不驱逐）——
**使用计数衰减 + 环形指针面**缺失。

## Solution

`ClockSweepCache`（core/cache）：

- 环形帧序 + 时钟指针：`evictOne()` 自指针起扫描——usage=0
  即摘除返回；usage>0 递减后跳过（**衰减式第二机会**——
  常用页多扛几轮但不豁免）；
- 使用计数：装入=1、命中+1、封顶 {@value #MAX_USAGE}
 （PostgreSQL INT_MAX 口径的教学钳制——防奇高永驻）；
- `put` 覆盖已有键只更新值（帧位不变）；`get` 命中提升；
- 读数：size/containsKey/clockHand；驱动确定性（无时间依赖）；
- fail-fast：capacity≤0、null key。

## User Stories

1. 作为页缓存作者，偶发扫描不洗出热页、老计数页不再永驻。
2. 作为审计作者，同操作序列同驱逐序（确定性可回放）。

## Testing Decisions

- 基础驱逐序（全 1 计数顺时针摘头）；命中率差异显证
 （高频旧页存活、低频新页先出——LRU 反例对照）；使用计数
  封顶；空缓存 evict null；覆盖键不增帧；负容量/null
  fail-fast；确定性回放。

## Out of Scope

- 不做后台写回（脏页 flush 归调用方）；不做分区缓存；
- 不做 LRU 链表混成。

## Further Notes

- 与精确响应缓存（spec 53，LRU+TTL）同族不同面：使用计数
  环形衰减 vs 访问序 TTL。Wave 2 第五件。
- 里程碑：S10/50（20%）。
