# Spec 7019 — InvertedIndex 倒排索引（effort #7019，U20）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7239–U7240，impl 2271）。
> 借鉴：Lucene/Elasticsearch 词项→posting 布尔召回思想。

## Problem Statement

布尔检索的病：每查询全文档扫列 O(N·L)（语料放大）
——**词典命中后只走 posting 列表面**缺失。

## Solution

`InvertedIndex`（core/metrics）：词项→有序 posting；
AND=交集/OR=并集；非字母数字小写切分（确定性词法）；
重复 docId 幂等替换（内部静默卸载+remove 缺席 fail-fast
——勘误：初版 add 复用 fail-fast remove 首索引炸，钉住
修正）；DF/词典/文档数读数。

## Testing Decisions

- 手语料 AND/OR；幂等替换/删除统计一致；100 轮随机
  语料 vs 逐文档扫列圣像；fail-fast。

## Out of Scope

- 不做评分（BM25 面）；不做分词器扩展。

## Further Notes

- 与 MinHashSketch 同族不同面：精确词项布尔召回 vs 近似
  相似度签名。
- 里程碑：U20/50（40%）。
