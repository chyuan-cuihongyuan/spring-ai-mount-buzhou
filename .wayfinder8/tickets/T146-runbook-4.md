---
Type: task
Status: open
blocked-by: T133, T134, T136
---
## Question

runbook 第四轮增补：死信重放命令化、索引保留期调优、迁移操作步骤、health 新维度解读。

## Resolution

AFK 自决：§2 死信处置改 replayDeadLetters() 一键；§3 增 closed-retention；§5 增迁移步骤；§7 增 observability 丢弃指标（T139 若落）；health 解读注记。产 impl-119。
