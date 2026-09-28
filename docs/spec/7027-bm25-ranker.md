# Spec 7027 — Bm25Ranker 评分器（effort #7027，U28）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7255–U7256，impl 2279）。
> 借鉴：Okapi BM25（Robertson & Spärck Jones；Lucene/ES 默认）。

## Problem Statement

相关性排序的病：裸 TF 计数（长文档刷分）——**IDF 饱和
+TF 饱和+长度归一三件套面**缺失。

## Solution

`Bm25Ranker`（core/metrics）：score=Σ idf·tf·(k1+1)/
(tf+k1(1−b+b·|d|/avg))；k1≥0、b∈[0,1]（0=不归一——
长度增益消失、并列 canonical 按 docId——语义钉住）；
确定性切分/评分；幂等替换（静默卸载核——勘误：add 复用
fail-fast remove 首索引炸同 InvertedIndex，钉住修正）。

## Testing Decisions

- 排序手锚（TF 高者先）；b 开关效应（0.75 短文档先/0
  并列 canonical）；并列 docId 升序；fail-fast。

## Out of Scope

- 不做查询词加权；不做 BM25F 多域变体。

## Further Notes

- 与 InvertedIndex（7019）同族不同面：布尔召回 vs 相关性
  排序。
- 里程碑：U28/50（56%）。
