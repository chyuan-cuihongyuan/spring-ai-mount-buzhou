---
Type: task
Status: closed
---
## Question

CompensatingBatch 原语：倒序补偿、补偿失败即停、每步事务内执行、观测计数。

## Resolution

done（2026-09-01）：impl-327；core.transaction.CompensatingBatch（Step.of +
run(uow, steps)；compensated/compensation-failed 双计数）。四用例回归绿。
