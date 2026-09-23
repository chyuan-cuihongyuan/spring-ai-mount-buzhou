# Spec 4044 — Rope 文本缓冲（effort #4044，R45）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6089–T6090，impl 2145）。
> 借鉴：Rope 树状文本缓冲（xi-editor/ropey 思想）。

## Problem Statement

大文本编辑的病：字符串拼接/删除全量拷贝（O(n) 每操作——
O(n²) 编辑序列）、或裸数组位移（同样全量搬移）——**分块
树状增量编辑面**缺失。

## Solution

`RopeBuffer`（core/policy）：

- 权重平衡二叉树：叶持文本块（≤ 512 字符），内部节点
  weight = 左子树长度——charAt/insert/delete 按权重导航；
- insert/delete 经 split+concat 组合；超限深度触发**重建**
 （叶中位切分重排——确定性再平衡）；
- 读数面：length()/depth()（平衡性证据）；
- fail-fast：charAt/insert 偏移越界、delete 区间非法
 （start>end / 越界）。

## User Stories

1. 作为大文档编辑器作者，编辑序列不再 O(n²) 全量搬移。
2. 作为审计作者，同操作序列同结果（确定性可回放）。

## Testing Decisions

- 固定种子 500 操作序列 vs StringBuilder 圣像（toString 与
  随机 charAt 双等）；头/中/尾插入与跨块删除显例；深度上界
 （≤ 2·log₂len + 常数）平衡证据；越界 fail-fast。

## Out of Scope

- 不做持久化结构（immutable version tree）；不做 UTF-8 字节
  口径（char 口径）；不做 b-tree 变体（ropey 同款）。

## Further Notes

- 与 ThreeWayMerge 同族不同面：编辑缓冲 vs 合并对账。
  Wave 8 第三件。
- 里程碑：45/50。
