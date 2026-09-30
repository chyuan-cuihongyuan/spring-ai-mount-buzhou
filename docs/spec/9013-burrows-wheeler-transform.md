# Spec 9013 — Burrows-Wheeler Transform 可逆重排（effort #9013，W14）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9027–W9028，impl 2366）。
> 借鉴：Burrows-Wheeler 1994（bzip2 块预处理同源——Fenwick 聚簇前置思想）

## Problem Statement

直接熵编码吃不到上下文冗余的病：重复模式串
散布全文——**BWT 可逆重排**：旋转排序末列同上下文
聚簇，为 MTF/RLE 铺路。

## Solution

BurrowsWheelerTransform（core/message，静态
工具面）：transform→BwtResult(末列,primaryIndex)；
inverse LF 映射重建；无哨兵；确定。

## Testing Decisions

ab/aab 手锚；重复串聚簇圣像（相邻同字符对
对比）；6 文本+60 随机往返；确定性；fail-fast。

## Out of Scope

不做后端压缩（MTF/RLE/Huffman 消费方组合）；
不做后缀数组加速（O(n²logn) 朴素面——块尺寸
明示）；不做哨兵字节变体。

## Further Notes

与 MoveToFrontTransform（9014）组成 bzip2 前半
管线。Wave 3 第二件。
