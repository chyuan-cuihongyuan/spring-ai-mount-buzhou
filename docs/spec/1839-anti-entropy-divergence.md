# Spec 1839 — 反熵分歧账（effort #1839，R40）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2879–T2880，impl 1440）。借鉴：
> Cassandra/Dynamo anti-entropy repair + read repair——副本健康=数据一致，
> 分歧三型分开数，修复批量由分歧账说话。

## Problem Statement

主副本一致性只有「有没有对账过」的布尔记忆：分歧**长什么样**（副本丢写？
主被清？版本冲突？）没有账面——三型分歧的修复动作完全不同（移交重放/
对账裁决/冲突解），混在一起则修复批排不了优先级。

## Solution

`AntiEntropyDivergence`（core/recovery，静态纯函数）：

- `compare(primaryVersions, replicaVersions)` → `Divergence(onlyInPrimary,
  onlyInReplica, versionMismatch, matched)`：键×版本比对四桶；
- `repairWorkload()` = 三型合计（反熵修复批量依据）；
- `matchedRatio()` 一致率（空比对 -1 哨兵）。

## User Stories

1. 作为存储运维者，onlyInPrimary=2000 → 副本丢写 2000 键，移交/重放批
   该起；versionMismatch=3 → 冲突解队列只有 3 件，不慌。
2. 作为对账编排者，matchedRatio 周环比下滑 = 分歧在长，反熵周期该缩。
3. 作为框架宿主，键与版本口径（世代号/序列号）自声明，纯读面零修复。

## Implementation Decisions

- 纯读不修复（三型修复动作归宿主）；null 任一按空表。

## Testing Decisions

- 四桶分开数+工作量+一致率；全一致健康态；空比对/单侧空哨兵。

## Out of Scope

- 不执行修复；不做 Merkle 树差分优化（键域分片归未来静脉）。

## Further Notes

- 与 GapBackfillPlanner 配对：分歧账定「多少」，缺口单定「哪段」。
