# Spec 4003 — Boyer-Moore 多数表决（effort #4003，R4）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6007–T6008，impl 2104）。
> 借鉴：Boyer-Moore MJRTY 1991（配对抵消多数投票）。

## Problem Statement

找「超半数元素」（灰度版本一致性/评估标注一致性/错误签名主导性）
排序取中位数 O(n log n)、哈希计数 O(n) 内存——**单遍常数内存**
无件可用。

## Solution

`BoyerMooreMajority`（core/metrics，纯静态）：

- 配对抵消：候选 + 计数器，同候选 +1、异候选 −1、归零换候选；
- 真多数（> n/2）必幸存（每消一对至多废一张多数票）；幸存者
  未必多数——二次核验定夺；
- 恰半是僵局不是多数（投票语义）；空表/无多数 → null。

## User Stories

1. 作为灰度作者，配置版本是否超半一眼可判——常数内存单遍。
2. 作为评估作者，标注一致性多数派提取免全量计数。

## Testing Decisions

- 真多数幸存直读；恰半与三分散 null；抵消重立 + 围剿否决两向；
  空表/单元素/泛型整型直证；null 表 fail-fast。

## Out of Scope

- 不做超 1/k 频繁项（MisraGries/SpaceSaving 已覆盖）；
  不做加权票。

## Further Notes

- 与 MisraGriesSketch 成对：k=1 特例的确定性精确版（无近似）。
- 里程碑：4/50。
