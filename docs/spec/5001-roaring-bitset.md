# Spec 5001 — Roaring 压缩位图（effort #5001，S2）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6103–T6104，impl 2152）。
> 借鉴：Roaring Bitmap 容器化压缩位图（RoaringBitmap/Lucene/Spark 思想）。

## Problem Statement

大整数集合的病：裸 `long[]`/`HashSet` 存稠密 id（内存爆炸）
或裸 `boolean[]` 位图（稀疏 id 同样爆炸）——**分容器自适应
压缩面**缺失。

## Solution

`RoaringBitSet`（core/metrics）：

- 高 16 位分桶（container key），桶内自适应：稠密桶
  （≥ 4096 元素）`long[4096]` 位图、稀疏桶短整型有序数组
 （Roaring 阈值口径）；
- `add/remove/contains` O(桶内 log n / O(1))；`cardinality`
  增量计数；`and/or` 桶级交并（对齐 key）；
- 迭代序 = 无符号桶序 + 桶内升序（确定性）；
- fail-fast：负值/超 int 正域。

## User Stories

1. 作为标签/倒排作者，稠密段位图省内存、稀疏段数组不浪费。
2. 作为审计作者，同操作序列同 cardinality（确定性可回放）。

## Testing Decisions

- 圣像对拍（固定种子 1e5 次增删 vs HashSet 圣像：contains/
  cardinality 全等）；桶分裂阈值两侧（密转位图、疏转数组）
  显证；and/or 基数守恒；负值/越域 fail-fast；确定性回放。

## Out of Scope

- 不做 run-length 容器（Roaring 三型取二型）；不做序列化
  格式（Roaring 标准wire）；不做 long 域（int 域口径）。

## Further Notes

- 与 HllCardinalitySketch（基数估计）互补：精确压缩集合 vs
  概率基数。Wave 1（采样统计族）开波。
- 里程碑：S2/50（4%）。
