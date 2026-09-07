---
Type: task
Status: closed
---
## Question

yml 装配（buzhou.memory.idle-compaction.*，默认关）+ 依赖可缺（ObjectProvider
NullBean 语义）。

## Resolution

done（2026-09-01）：impl-333；IdleCompactionProperties record + memory autoconfig
conditional bean。
