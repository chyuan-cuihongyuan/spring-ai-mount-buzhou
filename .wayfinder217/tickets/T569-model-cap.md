---
Type: task
Status: closed
---
## Question

轮内模型调用总闸：(session, turn) 计数超上限 block。

## Resolution

done（2026-08-30）：impl-312；ModelCallCapHook（order 20；afterTurn 主动清；
LRU 1024；blocked 计数）。
