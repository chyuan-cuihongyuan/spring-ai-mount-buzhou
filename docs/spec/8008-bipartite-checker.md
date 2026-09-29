# Spec 8008 — BipartiteChecker 二分图染色（effort #8008，V9）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8017–V8018，impl 2310）。
> 借鉴：Kőnig 1931 二分图定理（任务分组/冲突排除同源思想）。

## Problem Statement

冲突二分排除的病：奇圈存在则二染色必撞色——**暴力枚举
双集合划分 O(2^V)** 不可承受；BFS 交替染色一次线性判定。

## Solution

`BipartiteChecker`（core/concurrent，静态工具面）：BFS/DFS
交替 0/1 染色，撞色即非二分；`isBipartite` 判定 + `sides`
双集合（非二分返回空——诚实缺省）；自环 fail-fast（自指
节点自相矛盾）；孤立节点归 0 侧 canonical（确定性）；节点
越界 fail-fast。

## Testing Decisions

- 手锚（偶圈/奇圈/树/星形/两点无边）；Kőnig 性质：无奇圈
  ⇔ 二分（200 随机图 vs 三角枚举找奇圈暴力圣像判定全等）；
  sides 双射覆盖+互斥性质；fail-fast。

## Out of Scope

- 不做最大匹配数（匈牙利面另件）；不做连通分量分组枚举。

## Further Notes

- 与 GraphColoring（8010，V11）同族不同面：二染色判定 vs
  k=Δ+1 贪心着色最小色数近似。
- 里程碑：V9/50（18%）。
