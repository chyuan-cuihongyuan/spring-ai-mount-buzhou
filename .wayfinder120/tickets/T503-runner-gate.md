---
Type: task
Status: closed
---
## Question

期望门禁接线：EvalRunner setExpectations + run 前校验 fail-fast。

## Resolution

done（2026-08-30）：impl-287；EvalRunner 门禁装载 + 脏数据零模型调用 +
message 带发现明细前 3 条 + 红队 3 例 + 既有回归。
