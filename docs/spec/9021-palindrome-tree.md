# Spec 9021 — Palindrome Tree 回文树（effort #9021，W22）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9043–W9044，impl 2374）。
> 借鉴：Eertree（Apostolico 1995/Gusfield 同源——互异回文在线收集经典结构）

## Problem Statement

逐中心枚举 O(n²) 收集互异回文、Manacher 只出
单一最长——**Eertree**：每回文恰一态双根在线
构造，增量一字符至多新增一态。

## Solution

PalindromeTree（core/metrics，实例类）：of(text)；
distinctPalindromeCount/longestPalindromeLength/
allPalindromes/palindromeFrequencies。

## Testing Decisions

abba/aaaa 锚；空/单字符退化；30 随机文本
对拍暴力（互异集+最长）；互异≤n 结构界；
确定性；fail-fast。

## Out of Scope

不做 Unicode 特殊情形区分（一般 char 域）；
不做回文切割数（回文 DP 面）；不做增量追加
查询（在线构造态——追加留后续）。

## Further Notes

与 Manacher 同域不同面；与 SuffixAutomaton
（9020）同构不同面。Wave 4 第四件。
