# 605 — 技能混合排序（BM25 + 语义 RRF 融合）

> 借鉴：[weaviate](https://github.com/weaviate/weaviate) / [Qdrant](https://github.com/qdrant/qdrant) hybrid search（BM25 + 向量的 RRF 融合）。
> 来源：F 会话第 6 轮 = effort #600 / [T860](../../.wayfinder/tickets/T860-hybrid-skill-rank-shape.md) / [T861](../../.wayfinder/tickets/T861-hybrid-skill-rank-verify.md) / impl 458。

## 背景

技能目录语义排序（spec 59，embedding cosine）对**精确词**判别弱——「E429」「DEPLOY-123」这类型号/错误码/专有名词在向量空间里与同义描述混作一团。向量库的解法是 hybrid：词法 BM25 与语义两路打分融合。

## 目标

1. `LexicalSkillRanker`：BM25 词法排序（k1=1.2 / b=0.75 经典值；分词 = ASCII 词小写化 + CJK 连续段 bigram、不跨界）。
2. `HybridSkillRanker`：语义 + 词法两路 RRF 融合（`Σ w/(k+rank)`，k=60；权重可配默认 1:1）——免分数量纲对齐。
3. 语义路嵌入失败 → 纯词法序 + `semanticFallbackCount()` 观测（注入链路不断）。

## 非目标

- 不改默认装配/排序行为（opt-in，与 spec 59 语义排序并存）。
- 不做 BM25 参数化与索引结构（候选集 = 目录量级，逐次打分够用）。
- 不做查询改写/同义扩展。

## 测试

HybridSkillRankerTest 6 用例：词法命中（错误码/CJK bigram/tf 饱和）、RRF 分歧融合与加权翻转、语义降级、tokenize 边界、守卫与构造校验。

## 兼容性

纯增量两个新类；SemanticSkillRanker 未动（其既有测试零回归）。
