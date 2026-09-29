# Spec 8034 — JaroWinklerSimilarity（effort #8034，V35）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8069–V8070，impl 2336）。
> 借鉴：Jaro 1989/Winkler 1990 记录链接思想（美普查局）。

## Problem Statement

短串相似的病：编辑距离对换位不敏感——**Jaro 匹配窗+换位比，Winkler 前缀加成**——人名链接标准。

## Solution

JaroWinklerSimilarity（core/metrics）：jaro 匹配窗 ⌊max/2⌋−1+换位率+winkler 前缀 ≤4 加成系数可配+similarity [0,1]+null fail-fast+对称性/确定性。

## Testing Decisions

MARTHA/MARHTA=0.961（勘误经圣像校核）+DIXON/DICKSONX=0.813 官方值+前缀加成单调+对称性+fail-fast。

## Out of Scope

- 不做在线增量/分布式面（单机批语义明示）。

## Further Notes

- 里程碑：V35/50。
