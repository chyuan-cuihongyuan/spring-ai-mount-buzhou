# Spec 6009 — Suffix Array 后缀数组（effort #6009，T10）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6219–T6220，impl 2210）。
> 借鉴：Manber-Myer 倍增构造 + Kasai LCP 思想（Lucene/生信
> 索引同源）。

## Problem Statement

静态文本子串查询的病：每查询对全部后缀 startsWith 线性扫
（O(n·m) 每查放大）——**一次构建多查询索引面**缺失。

## Solution

`SuffixArray`（core/metrics）：

- 倍增构造（rank 对排序，O(n log²n)）+ Kasai LCP（O(n)）；
- `contains`/`occurrenceCount` 二分后缀序（O(m log n)），
  重叠出现全计；
- 读数：suffixArray()/lcpArray() 防御性副本；fail-fast：
  null/空文本、null/空查询。

## User Stories

1. 作为检索作者，静态文本建一次索引——任意子串对数查询。
2. 作为审计作者，后缀序+LCP 可独立复算——结构可审计。

## Testing Decisions

- banana 经典 sa/lcp 全序钉住；300 字母随机文本 100 查询
  contains/计数 vs text.contains/indexOf 暴力全等；后缀序
  两两有序 + LCP 随机 50 对直算全等；fail-fast。

## Out of Scope

- 不做在线构造/流式追加（静态定构）；不做最长重复子串等
  衍生查询（调用方以 sa/lcp 自算）。

## Further Notes

- 与 AhoCorasick（T7）同族不同面：模式集索引文本 vs 文本
  索引查询集；与 KmpSearch 不同面：单模式流扫 vs 静态
  多查询。
- 里程碑：T10/50（20%）。
