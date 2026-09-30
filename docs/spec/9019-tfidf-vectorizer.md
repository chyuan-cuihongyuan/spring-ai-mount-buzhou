# Spec 9019 — TF-IDF Vectorizer 词频-逆文档频率向量化（effort #9019，W20）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9039–W9040，impl 2372）。
> 借鉴：TF-IDF（Salton 1988——scikit-learn TfidfVectorizer/Solr/ES 同源；原 k-shingle 已占（NgramExtractor）换替补）

## Problem Statement

词袋计数无鉴别力、语义嵌入黑箱——**TF-IDF**：
词频×逆文档频率，常见词压权、鉴别词抬权的
可解释稀疏向量。

## Solution

TfIdfVectorizer（core/metrics，实例类）：of(docs)
建；vocabulary/idf/vector(tokens)/cosineSimilarity；
sklearn 平滑公式；词表升序位序契约。

## Testing Decisions

平滑公式手锚；共现词压权圣像；同主题>跨
主题余弦+正交；确定性；fail-fast。

## Out of Scope

不做查询相关性打分（Bm25Ranker 面）；不做
n-gram 特征化（NgramExtractor 已占）；不做子词
TF（sublinear_tf 面）。

## Further Notes

与 Bm25Ranker 同域不同面；W20 原选 k-shingle
已占（NgramExtractor）换替补（占坑复核勘误）。
Wave 4 第二件。
