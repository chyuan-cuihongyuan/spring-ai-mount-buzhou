---
Type: task
Status: closed
---
## Question

SpawnGate 三级优先级：高优先级抢占低级排队者、同级 FIFO。

## Resolution

done（2026-08-30）：impl-272；SpawnPriority（HIGH/NORMAL/LOW）+ SpawnGate 锁化改写
（每级票据队列 + 释放有向交接）；默认调用 = NORMAL 零行为变化。
