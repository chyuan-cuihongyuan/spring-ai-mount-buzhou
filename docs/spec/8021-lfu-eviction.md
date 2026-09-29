# Spec 8021 — LfuEviction LFU 驱逐（effort #8021，V22）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8043–V8044，impl 2323）。
> 借鉴：O'Neil, Graefe & Koniassek 1993 LFU（Redis allkeys-lfu 思想）。

## Problem Statement

缓存放逐的病：LRU 只看时新性——**偶发扫描把热键冲出；
LFU 按访问频次放逐（频次桶+桶内入桶序）**。

## Solution

`LfuEviction`（core/cache）：键→频次表 + 频次桶
（LinkedHashSet——同频取先入桶者 canonical）；`get` 命中
升频/`put` upsert 覆值不增位、满容量逐「最低频次+最先入
桶」键并返回被逐键（无逐出返回 null）/`peek` 不升频读/
`size` 读数；null 键值 fail-fast；容量 ≥1 越域 fail-fast；
确定性（同操作序同放逐）。

## Testing Decisions

- 手锚（容量 3：A,B,C 轮询+D 逐最早入桶低频者）；升频
  保键（A 两次后 B 先被逐）；upsert 不逐出；500 随机 vs
  暴力扫描圣像（min 频次+最早入桶序）被逐键全等；fail-fast。

## Out of Scope

- 不做频次老化衰减（FrequencySketch 家族另件）；不做
  TTL。

## Further Notes

- 与 SieveCache/LruKEviction（cache）同族不同面：频次
  放逐 vs 时新性/插手位图放逐。
- 里程碑：V22/50（44%）。
