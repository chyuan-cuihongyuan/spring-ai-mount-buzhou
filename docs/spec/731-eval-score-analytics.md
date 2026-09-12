# 731 — 评估分数分布解析

> 来源：G 会话第 32 轮 = effort #731（714 分数留痕的消费端）/ [T1062](../../.wayfinder/tickets/T1062-eval-score-analytics.md) / [T1063](../../.wayfinder/tickets/T1063-eval-score-analytics-verify.md) / impl 631。

## Problem

714 similarity 判定在每条 detail 留了分数——但「这批 run 的相似度分布」没人解析：调阈值（0.8→0.7 会多放行多少）全靠感觉。

## Solution

`EvalScoreAnalytics.similarityScores(EvalRunResult)` 纯函数：正则提取 detail 中的 similarity= 分数 → Report(scored, min, max, mean, scores 原序)。无分数口径项（EXACT/CONTAINS/[MEMO] 复用的 exact）跳过——scored 诚实计数。空集统计 NaN（区别于 0 均值）。

## Out of Scope
阈值修改建议（人裁）；跨 run 分数对比（漂移族 718 管通过率、分数族后续）。
