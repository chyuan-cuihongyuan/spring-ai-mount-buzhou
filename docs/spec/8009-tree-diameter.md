# Spec 8009 — TreeDiameter 树的直径（effort #8009，V10）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8019–V8020，impl 2311）。
> 借鉴：双 BFS/DP 经典（网络拓扑最长链规划同源思想）。

## Problem Statement

树最长链的病：全点对最短路取 max O(V²)——**树形 DP
（子树内最长+次长拼链）一次后序遍历 O(V)** 得直径。

## Solution

`TreeDiameter`（core/concurrent，静态工具面）：后序 DP——
每节点保子树内最深与次深（top1/top2），直径 = max(top1+top2)
跨节点拼链；`diameter` 长度 + `path` 直径端点序（端点
canonical：并列取小编号——同树同结果）；无权边语义（边
长 1）；null/空树 fail-fast（单节点 0 合法）；非树边
（多点父/环）构建期校验 fail-fast。

## Testing Decisions

- 手锚（链形 n、星形 2、双叉平衡树 4、单节点 0）；200
  随机树 vs 全点对 BFS 圣像 max 全等；path 端点距 == 直径
  性质；fail-fast。

## Out of Scope

- 不做带权边（无权语义明示）；不做所有直径枚举。

## Further Notes

- 与 LcaLifting（7010）同族不同面：祖先查询面 vs 全局
  最长链面。
- 里程碑：V10/50（20%）。
