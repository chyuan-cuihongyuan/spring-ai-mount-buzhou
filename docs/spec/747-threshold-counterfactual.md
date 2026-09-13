# 747 - 相似度阈值反事实对照

> 来源：G 会话第 48 轮 = effort #747（731 分数解析的决策端）/ [T1062](../../.wayfinder/tickets/T1062-eval-score-analytics.md) 票闭环沿用 / impl 631 续。

## Problem
731 给了分数分布——但决策要的是反事实：「阈值调到 0.6 会多放行几条」。人工拿分布心算易错。

## Solution
EvalScoreAnalytics.passesAtThresholds(run, thresholds...)：给定候选阈值集分别计算通过数（Map<Double,Integer>，入参顺序）。分数来源同 similarityScores 解析口径。null run fail-fast。

## Out of Scope
自动推荐阈值（人裁）；跨 run 对照（漂移族）。
