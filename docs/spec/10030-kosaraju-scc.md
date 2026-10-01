# Spec 10030 — KosarajuScc 双 DFS 强连通分量（effort #10030，X31）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10061–X10062，impl 2433）。
> 借鉴：Kosaraju–Sharir 思想——CLRS 22.5/网络科学同源（TarjanSccFinder 已占异面：单次 lowlink）

## Problem Statement

单次 DFS lowlink（Tarjan 已占）之外的强连通分量第二法——两次平凡 DFS 的分治直读面。

## Solution

KosarajuScc（core/concurrent）：components(n,edges)——正图迭代 DFS 显式栈得完成序、逆图按完成序逆序 DFS 收桶成 SCC；每分量顶点升序、分量表按最小顶点序（确定性口径）；null 边列表/null 边/端点越界 fail-fast。

## Testing Decisions

CLRS 22.5 经典 8 顶点手锚（分量与 TarjanSccFinder 交叉互证）+空边全孤立+全自环+100 顶点随机图与 Tarjan 分区相等圣像+确定性+fail-fast 四面。

## Out of Scope

不做增量维护面（动态加边另立）；不做分量 DAG 压缩图输出（消费方自组）。

## Further Notes

与 TarjanSccFinder（已占）同域不同面：双 DFS 完成序 vs 单 DFS lowlink——算法谱系互补面；Wave 6 图结构进阶族首件。
