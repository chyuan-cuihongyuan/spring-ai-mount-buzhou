# Spec 4043 — Git 提交图世代号（effort #4043，R44）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6087–T6088，impl 2144）。
> 借鉴：Git commit-graph generation number（O(1) 祖先裁决剪枝）。

## Problem Statement

依赖图血缘判定的病：裸时间戳比对（时钟偏斜下误判——Git
历史上 `commit-date` 启发式被时钟倒挂打爆的教训）或全图
遍历（大图代价爆炸）——**精确世代号剪枝面**缺失。

## Solution

`CommitGraph`（core/policy）：

- 注册面：`add(id, parents)` 拓扑序注册（未知父/重复 id
  fail-fast——construction-order 即 DAG-by-construction）；
- 世代号：gen(v) = max(父 gen) + 1，根 = 1——O(1) 读；
- 祖先裁决 `isAncestor(a, b)`：gen(a) ≥ gen(b) → **确定非
  祖先**（世代号剪枝快道——Git commit-graph 核心收益）；否则
  有界 BFS（只走 gen > gen(a) 的节点）精确判定；
- 世代号 vs 时间戳：精确单调、无时钟偏斜、可比对；
- fail-fast：未知 id 读数。

## User Stories

1. 作为血缘判定作者（会话派生链/知识版本链），O(1) 剪枝
   排除不可能祖先—— walks 不爆炸。
2. 作为审计作者，同图同判定（确定性可回放）。

## Testing Decisions

- 线性链世代号 1/2/3 与祖先双向判定（含剪枝快道返回）；
  菱形合并 max+1；倒挂时间戳构造（gen 裁决正确拒）对照
  时间戳启发式误判；未知父/重复 id/未知 id fail-fast。

## Out of Scope

- 不做 commit-graph 文件序列化（.git/commit-graph 格式）；
- 不做增量分片（incremental files）；不做 reachability bitset。

## Further Notes

- 与 TopologicalSorter 同族不同面：全序排程 vs 血缘裁决。
  Wave 8 第二件。
- 里程碑：44/50。
