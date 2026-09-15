---
id: T2651
title: 检索命中排名读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

RetrievalRankStats 的形状怎么裁决？（spec 1725 / effort #1725 / R26）（spec 1725 验收/裁决）

## Resolution

实例面 record(rank)（1 基，<=0 miss）+report(hits/misses/mrr −1 哨兵/hitAt1/hitAt3)，MRR 千分位累加防浮点累漂——ES rank_eval/MRR 思想，最终被用条目的排名质量。
