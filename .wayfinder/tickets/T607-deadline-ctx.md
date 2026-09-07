---
Type: task
Status: closed
---
## Question

ToolContext 携带 TurnDeadline（动态视图）+ turnDeadlineOf 取用助手。

## Resolution

done（2026-09-01）：impl-331；管理器置 `buzhou.turnDeadline` 键 + 静态助手
（缺键/异型 → none() 哨兵）。DeadlinePropagationTest 三用例绿。
