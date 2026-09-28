# Spec 7010 — LcaLifting 倍增 LCA（effort #7010，U11）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7221–U7222，impl 2262）。
> 借鉴：二倍增祖先跳思想（ competitive programming/代际树同源）。

## Problem Statement

树祖先查询的病：每次查询沿父链爬升 O(depth)（深树反复
查询放大）——**2^k 倍增表 O(n log n) 预处理 O(log n)
查询面**缺失。

## Solution

`LcaLifting`（core/concurrent）：

- up[k][u]=2^k 级祖先逐层倍增拼表；深度对齐+同层同步
  上跳；kthAncestor/depth/distance 一等读数；k 越深度域
  fail-fast（根之上无祖先诚实拒绝）；
- 构建期树形校验（单亲/可达/build 前后互锁）fail-fast；
  确定性无随机。

## User Stories

1. 作为组织树作者，任意两节点最近公共祖先 O(log n)。
2. 作为审计作者，随机树 vs 父链爬升暴力圣像全等。

## Testing Decisions

- 七节点满二叉树手锚；100 随机树 ×30 查询 vs 暴力爬升
  圣像（LCA/深度/kth 全等）；build 生命周期互锁；fail-fast。

## Out of Scope

- 不做欧拉序+RMQ 变体；不做动态插删。

## Further Notes

- 与 TopologicalSorter 同族不同面：DAG 序面 vs 树祖先
  查询面。
- 里程碑：U11/50（22%）。
