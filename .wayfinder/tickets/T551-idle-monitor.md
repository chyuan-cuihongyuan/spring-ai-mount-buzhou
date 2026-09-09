---
Type: task
Status: closed
---
## Question

空闲水位判定 + 翻转通知 + 候选清单。

## Resolution

done（2026-08-30）：impl-303；IdleSessionMonitor（特征仓快照只读 + 空闲名册
LRU 1024 + entered 计数 + 空闲时长降序）。
