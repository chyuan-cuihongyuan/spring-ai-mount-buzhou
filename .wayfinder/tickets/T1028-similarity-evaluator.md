---
id: T1028
title: 相似度阈值判定器的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

内置三判对 LLM 输出词序微变脆判——加确定性模糊判定吗？分数放哪？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 15 轮 = effort #714 / spec 714 / impl 614）：`BuiltInEvaluators.similarity(minRatio)`——字符 trigram 集合 Jaccard（语言无关+大小写不敏感+空白折叠），ratio≥阈值 pass（含边界），detail 携带 `similarity=0.xxxxxx 阈值=x` 分数留痕（schema 不动，漂移分析可解析）；minRatio∈[0,1] fail-fast；退化口径双方空=1 单方空=0。语义相似度归嵌入/judge 族（#7 边界）。
