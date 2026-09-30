# Spec 10021 — DeBruijnAssembler（effort #10021，X22）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10043–X10044，impl 2424）。
> 借鉴：de Bruijn 图欧拉路径组装（Idury–Waterman 1995/SPAdes–Velvet 同源）

## Problem Statement

基因组组装需从读段重构序列——全比对重叠排序 O(n²) 的病解——k-mer 边化欧拉路径重构。

## Solution

DeBruijnAssembler（core/concurrent，静态纯函数面）：assemble(reads,k)——(k−1)-mer 顶点+k-mer 有向边、欧拉起点判定（度差 +1/−1）+Hierholzer 标准栈式路径+非欧拉（分支/断链/度差顶点数）fail-fast；邻接 TreeMap 确定序。

## Testing Decisions

完美重叠重构圣像（ACGTTGCAAT k=4）+读段序无关+重复 k-mer 重边基因组+非欧拉两面 fail-fast+契约四面。

## Out of Scope

不做读段纠错/简化（tips/bubbles 另立）；不做双端校验（paired-end 另立）；不做环形基因组（回环契约另立）。

## Further Notes

与 CenterStarAligner（10022）同族不同面：图重构 vs 多序列比对；开发勘误三处入档（弹栈序漏逆置+imports 误删+重复读段倍增破欧拉测试口径）。
