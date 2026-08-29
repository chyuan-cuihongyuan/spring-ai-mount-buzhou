---
Type: task
Status: closed
---
## Question

端到端轮时延计时 + timer 喂数 + 滚动窗读数。

## Resolution

done（2026-08-30）：impl-309；TurnTimingHook（nanoTime 单调钟 + 滚动 64 +
LRU 1024 + 重入覆盖安全）。
