# Spec 1851 — 桶表容量阶梯（effort #1851，R52）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2903–T2904，impl 1452）。借鉴：
> HashMap 负载因子 + 2 的幂容量惯例——负载因子触发扩容（碰撞链超线性
 *拐点），2 的幂基数配位与取模。

## Problem Statement`

自建桶表（哈希索引/分桶缓存）的容量决策靠拍：初容量随手、何时扩容看
心情——装填度过高后碰撞链超线性（读退化 O(链长)），过低又浪费内存
——「建议容量/扩容判定/装填度」三件套缺基建。

## Solution

`BucketTableSizing`（core/cache，静态纯函数）：

- `suggestCapacity(expectedEntries, loadFactor)`：⌈n/lf⌉ 向上取 2 的幂
 （0 条目 → 1 最小幂）；
- `verdict(capacity, size, loadFactor)` → `OK / RESIZE_NEEDED`（装填度 ≥
  负载因子即扩，边界含——到线即扩不留侥幸）；
- `load(capacity, size)` 装填度读数；默认负载因子 0.75 常量。

## User Stories

1. 作为索引作者，预期万级条目 → suggestCapacity(10000, 0.75)=16384 一行
   定初容，位与取模直接用。
2. 作为缓存治理者，verdict 翻 RESIZE 即 rehash——碰撞链超线性拐点前动手。
3. 作为容量审计者，load 读数进指标——装填度趋势即扩容前瞻。

## Implementation Decisions

- 纯建议不扩容（rehash 归宿主）；2 的幂循环左移防溢出上界（long 中间量）。

## Testing Decisions

- 建议幂四例（含 0 条目与 lf=1）；扩容边界含（3/4 vs 2/4）；装填读数；
  畸形五型 fail-fast。

## Out of Scope

- 不实现 rehash；不做并发扩容语义（那是 ConcurrentHashMap 族归未来静脉）。

## Further Notes

- 与 SessionBloomFilter 正交：那是「有没有」概率结构，这是确定桶表容量面。
