---
Type: task
Status: closed
blocked-by: T190-eval-dataset-store.md, T191-feedback-import.md, T192-evaluator-spi.md, T193-eval-runner.md, T194-eval-query.md, T195-eval-events.md
---
## Question

术语节（评估数据集/回流/评估器/run）+ effort#11 公共面（含破坏性变更入档，如无则钉住零变更）。

## Resolution

impl-166 落地：术语节 6 条；api-surface effort#11 节（core.eval 全新包 + emitEvent +
ErrorCode.EVAL_OPERATION_INVALID + isNegative/decode 可见性提升）；破坏性变更钉住零。
T200 关闭。
