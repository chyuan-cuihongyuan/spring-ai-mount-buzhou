# Spec 5034 — Radix Tree 基数树最长前缀路由（effort #5034，S35）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6169–T6170，impl 2185）。
> 借鉴：go-chi/httprouter 静态路由基数树（压缩前缀树思想）。

## Problem Statement

前缀路由的病：逐键 `startsWith` 线性扫（每查 O(键数×键长)，
路由表大即放大）或朴素 Trie（每字符一节点——内存放大）——
**压缩结构 + 最长前缀匹配面**缺失。

## Solution

`RadixTree<V>`（core/policy）：

- 压缩前缀树：单字符边合并为字符串边（公共前缀只存
  一份——节点数=分支点数而非字符数）；
- `insert`：沿边行走，部分命中即**分裂出中间节点**
  （静态建树语义——终态与插入序无关）；
- `match`：最长前缀匹配（输入尽深处最近的终节点胜出）；
- 读数：keyCount/nodeCount（压缩度可见）；
- fail-fast：null/空键、null 值、重复键、null 输入。

## User Stories

1. 作为路由作者，键表大时每查只走键长深度——不再线性扫。
2. 作为结构审计者，nodeCount 压缩度读数——朴素 Trie 内存
   放大可见。

## Testing Decisions

- 最长前缀胜出（/api/v1/users/42 → /api/v1/users；/ap 缺
  配空回）；部分命中边分裂（roman/romane/rome 经典族：
  romanes → romane、roman empire → roman）；终态与插入序
  无关（正序/逆序建树 nodeCount=4 全等 + 探针同答）；
  压缩节点数钉住（abcdef/abcxyz → 4 节点 2 键）；畸形
  fail-fast。

## Out of Scope

- 不做删除/再平衡（静态路由表面）；不做参数化路由段
 （:param 通配——本件是纯前缀语义面）；不做优先级加权
  匹配。

## Further Notes

- 与 JumpHash（spec 5030）同族不同面：前缀结构匹配 vs
  无状态散列分布。Wave 6 第五件。
- 里程碑：S35/50（70%）。
