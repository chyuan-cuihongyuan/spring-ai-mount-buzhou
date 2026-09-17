# Spec 3028 — CLOCK 二次机会驱逐缓存（effort #3028，R29）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5057–T5058，impl 2029）。
> 借鉴：CLOCK/second-chance（近似 LRU 的经典页替换）。

## Problem Statement

FIFO 盲逐出（不分青红皂白逐最老）把热点也逐；严格 LRU 每命中
都要链表搬移（高频命中路径 O(1) 但维护贵、锁竞争热）。需要
 近似 LRU 的 O(1) 低摩擦件。

## Solution

`ClockEviction<K,V>`（core/cache，泛型）：

- 环形帧阵列 + 引用位：命中/更新置位；满载逐出时针扫描——
  位 1 清位跳过（**二次机会**）、位 0 逐出；
- 空闲槽顺填；`size/capacity/evictedCount` 对账读数；
- 容量 ≥1 校验；非线程安全（单线程口径）。

## User Stories

1. 作为缓存作者，命中路径零搬移（置位即完）——近似 LRU 低摩擦。
2. 作为对账作者，逐出计数守恒——写错可断。

## Testing Decisions

- 命中/未命中基础；100 写容量恒 ≤3 且逐出恰 97；二次机会**两代
  手迹**（变体 A 新条目带位进入：首代全 T 扫一圈清位逐首——
  FIFO 样；第二代引用过的幸存、未引用的被逐）；全引用环扫清位
  再逐首（c=2 双置位场景）；同键更新不增条目零逐出；两代插入
  压力（访问者 2/4 幸存、未访问的 3 被逐）；容量 0/负 fail-fast。

## Out of Scope

- 不做精确 LRU（需要精确序用 LinkedHashMap accessOrder 或既有
  LRU 件）；不做 GCLOCK（引用计数变体留白）；不做并发（单线程
  口径）。

## Further Notes

- 与 AdaptiveReplacementCache（自适应四链）互补：CLOCK 是**低
  摩擦近似**档，ARC 是**自适应精确**档——按维护成本选型。
- 里程碑：29/150。
