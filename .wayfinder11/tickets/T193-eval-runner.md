---
Type: task
Status: open
blocked-by: T190-eval-dataset-store.md, T192-evaluator-spi.md
---
## Question

EvalRunner：dataset → 逐项 spawn 合成评估会话（隔离命名空间）chat 执行 → 评估器打分 →
run 记录落 store（runId/dataset/itemResult 逐项）；顺序执行；单项失败记录不断批；
run 汇总（passRate）收尾写。
