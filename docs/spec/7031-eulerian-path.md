# Spec 7031 — EulerianPath 欧拉路径（effort #7031，U32）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7263–U7264，impl 2283）。
> 借鉴：Hierholzer 1873 后序栈拼路思想。

## Problem Statement

一笔画的病：回溯式暴力找路（指数级）——**度数判存在
+后序栈拼路面**缺失。

## Solution

`EulerianPath`（core/concurrent）：度数条件前置（奇度
0/2——存在性诚实拒绝，跑一半失败的病根排除）+Hierholzer
后序栈拼路；邻接升序确定性；路径边数=边总数（性质钉）；
重边合法（各走一次）；不连通 fail-fast。

## Testing Decisions

- 三角回路（逐边合法+首尾闭合）；四节点迹（起点 0 终
  点 1）；度数违反/不连通 fail-fast；边界。

## Out of Scope

- 不做混合图；不做最小代价闭路（中国邮差）。

## Further Notes

- 与 TopologicalSorter 同族不同面：DAG 依赖序 vs 每边
  恰走一次。
- 里程碑：U32/50（64%）。
