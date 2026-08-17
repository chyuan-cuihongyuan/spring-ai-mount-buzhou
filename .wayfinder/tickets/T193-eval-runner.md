---
Type: task
Status: closed
blocked-by: T190-eval-dataset-store.md, T192-evaluator-spi.md
---
## Question

EvalRunner：dataset → 逐项 spawn 合成评估会话（隔离命名空间）chat 执行 → 评估器打分 →
run 记录落 store（runId/dataset/itemResult 逐项）；顺序执行；单项失败记录不断批；
run 汇总（passRate）收尾写。

## Resolution

impl-159 落地：项粒度会话（eval-<runId>-i<itemId>）；error/fail/pass 三态逐项记录 + 汇总
passRate 收尾落 `eval.run.<runId>`（往返同构）；异常不断批；空集零项 run 合法；null 评估器
按违约 error 记。勘察纠偏：ScriptedChatModel.throwOnCall 每次 call 优先消费的时序陷阱
（测试改计数替身，runner 无改动）。3 测试绿。T193 关闭。
