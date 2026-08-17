# 160 — 评估结果汇总查询

**Parent:** spec 52 §E / [T194](../tickets/T194-eval-query.md)

**Status:** done

- [x] EvalQueryService：allRuns/runs(dataset)/run(runId)/latestRun(dataset)；
  EvalRunSummary 摘要行（无 items 明细——明细走单 run）
- [x] startedAt 倒序；未知 runId / 无 run dataset = Optional.empty；只读面零写方法
- [x] 1 测试绿（多 dataset 过滤 + 倒序 + 最新 + 明细 + 未知 empty）
