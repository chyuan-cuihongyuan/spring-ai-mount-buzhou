# effort #731 — 评估分数分布解析（714 消费端）

- 会话：G 会话 700 系第 32 轮 ｜ spec [731](../../../docs/spec/731-eval-score-analytics.md) ｜ 票 [T1062](../tickets/T1062-eval-score-analytics.md)/[T1063](../tickets/T1063-eval-score-analytics-verify.md) ｜ impl631
- 借鉴：—（714 分数留痕的消费兑现）

## 勘察（排重）

- 714 similarity 在 detail 留痕分数——无解析消费面；grep -i scoreDistribution/ScoreAnalytics 零命中。

## 决定

`EvalScoreAnalytics.similarityScores(EvalRunResult)` 纯函数：正则解析 detail 中 similarity= 分数→min/max/mean/scores；无分数项跳过（诚实计数 scored）；空集统计 NaN。

## 测试

三分数解析统计/无分数项跳过/空集 NaN/null fail-fast。
