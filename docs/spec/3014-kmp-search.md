# Spec 3014 — KMP 字符串搜索（effort #3014，R15）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5029–T5030，impl 2015）。
> 借鉴：Knuth-Morris-Pratt 失配函数（lps）。

## Problem Statement

敏感串/守卫规则/停用词匹配若朴素回退扫描，最坏 O(n·m)（主指针
反复回退）；Java indexOf 可用但 findAll（可重叠全命中）与失配表
读数缺失，且自有实现可对账可审计。

## Solution

`KmpSearch`（core/metrics，纯函数静态件）：

- `indexOf(text, pattern)` O(n+m)：lps 失配滑模式不回退主指针；
  空模式约定 0（JDK 同款）；
- `findAll` **可重叠**（命中后回退 lps 前缀继续——「aa」在
  「aaaa」命中 0/1/2）；空模式 fail-fast（无穷匹配诚实拒绝）；
- `failureFunction(pattern)` 公共读数（lps 表——对账/教学面）。

## User Stories

1. 作为守卫作者，敏感串匹配线性时间——长文本高频扫描不退化。
2. 作为审计作者，lps 表与命中清单可复算可对账。

## Testing Decisions

- 首配手算三例（ababd@10 / ll@2 / issip@4）；可重叠 findAll 两例
  （[0,1,2] / [0,2,4,6]）；无匹配 −1/空；空模式双约定；模式长于
  文本；CLRS lps 手算（ababaca→[0,0,1,2,3,0,1]）+ aaaa/abcd；
  200 随机对拍 JDK indexOf（二字母表高重叠压力）；每命中子串
  自证等值。

## Out of Scope

- 不做多模式（Aho-Corasick 留白——后续轮候选）；不做通配符/正则；
  不做大小写/归一化（归调用方预处理）。

## Further Notes

- 与 TextDistance（近似）/NgramExtractor（特征）互补：本件是
  **精确子串**匹配地基。
- 里程碑：15/150。
