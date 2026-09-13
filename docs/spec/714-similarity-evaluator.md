# 714 — 相似度阈值判定器

> 来源：G 会话第 15 轮 = effort #714（E 会话池「judge 分档评分扩展」落地方）/ [T1028](../../.wayfinder/tickets/T1028-similarity-evaluator.md) / [T1029](../../.wayfinder/tickets/T1029-similarity-evaluator-verify.md) / impl 614。

## Problem

内置判定器只有 EXACT/CONTAINS/regex 三判——LLM 输出的词序微变、同义替换、标点差异即误判 fail（脆判），逼得用户要么用昂贵的 LLM-as-judge（宿主自实现）要么忍受假阴性。需要一个确定性的**模糊判定**：文本大体相似即过，且给出可审计的分数。

## Solution

HELM grading scales 思想（≈10K star：判定从二值扩展到分数带）+ 标准 IR 相似度：

- `BuiltInEvaluators.similarity(double minRatio)`：
  - 相似度 = **字符 trigram 集合的 Jaccard 系数**（|A∩B| / |A∪B|，语言无关——CJK/拉丁同口径；大小写不敏感、空白折叠）；
  - `ratio >= minRatio` → pass（边界含等号）；detail 携带 `similarity=0.123456 阈值=0.8`——**分数留痕**（run 记录 schema 不动，后续漂移基线/分布分析可解析 detail）；
  - 退化：双方空 = 1.0（一致）；单方空 = 0.0；
  - `minRatio ∈ [0,1]` 构造 fail-fast。

## User Stories

1. 评测作者：`similarity(0.8)` 替代 EXACT——「用 Arc 浏览器打开邮件」vs「用arc浏览器打开邮件」不再误判 fail。
2. 分数审计：detail 里的分数可供后续做分数分布/漂移分析（与 R37 评估漂移基线衔接）。

## Implementation Decisions

- trigram 在归一化文本（lowercase、连续空白折叠）上取字符 3-gram 集合；文本短于 3 字符退化为字符集合本身。
- 分数格式化 6 位小数（detail 稳定可断言）。
- 纯静态判定（无状态无依赖）。

## Testing Decisions

- 全等 → 1.0 pass；完全无关 → 0.0 fail；大小写/空白差异仍高分；阈值边界含等号（0.5 vs 0.5000001）；minRatio 越界拒绝；空串退化口径。

## Out of Scope

- 语义相似度（嵌入/LLM-judge——边界沿用 #7）。
- EvalScore 增 score 字段（run 记录 schema 变更——本轮 detail 留痕，schema 扩展留后续）。
- 编辑距离/Levenshtein 族（trigram Jaccard 已覆盖廉价口径）。

## Further Notes
