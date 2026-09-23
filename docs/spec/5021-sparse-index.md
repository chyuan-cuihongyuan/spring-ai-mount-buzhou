# Spec 5021 — Sparse Index 稀疏索引（effort #5021，S22）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6143–T6144，impl 2172）。
> 借鉴：LSM/SSTable sparse index（块首键索引 + 二分定位）。

## Problem Statement

排序块数据的查找病：每键全索引（索引与数据同量级）或全块
线性扫（块大则读放大）——**块首键 + 二分定位面**缺失。

## Solution

`SparseIndex`（core/metrics）：

- 构造：块（blockId, firstKey）按 firstKey 升序注册
 （乱序/空 fail-fast）；
- `locate(key)`：二分找**最后一个 firstKey ≤ key** 的块——
  该块可能包含 key（稀疏索引只承诺范围不承诺存在）；key 早于
  首块 → -1（诚实不在）；
- 读数：blockCount/firstKeys；
- fail-fast：null/空块集、乱序、null key。

## User Stories

1. 作为块存储作者，每 N 键一条索引——索引量级 O(块数)。
2. 作为审计作者，同索引同定位（确定性可回放）。

## Testing Decisions

- 中块定位；早于首块 -1；块界 firstKey 精确命中；晚于末块
  落末块；乱序/空块集 fail-fast；确定性回放。

## Out of Scope

- 不做块内查找（块内有序线性/二分归存储层）；不做多级
  索引；不做删除合并。

## Further Notes

- 与 Bitcask 键目录（R15 内存全索引）同族不同面：全量内存
  目录 vs 稀疏块索引。Wave 4 第四件。
- 里程碑：S22/50（44%）。
