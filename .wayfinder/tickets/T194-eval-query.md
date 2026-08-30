---
Type: task
Status: closed
blocked-by: T193-eval-runner.md
---
## Question

EvalQueryService：run 列表（按 dataset 过滤）/ 单 run 明细（逐项 actual/expected/score）/
最新 run 查询；scanByPrefix 下推；只读面（无变更）。

## Resolution

impl-160 落地：四查询（allRuns/runs/run/latestRun）+ 摘要行（明细单查）；startedAt 倒序；
只读零写方法；未知 runId 与空 dataset Optional.empty。1 测试绿。T194 关闭。
