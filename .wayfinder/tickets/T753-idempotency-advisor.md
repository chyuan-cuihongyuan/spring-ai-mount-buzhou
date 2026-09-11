---
Type: task
Status: closed
---
## Question

`IdempotencyAdvisor`（+440）：advisor 参数 `buzhou.idempotency-key` 缺席
透传；同键重入重放首次终态响应（复用 ResponseCacheStore+isTerminal）；
流式聚合后写、取消/错误不写。

## Resolution

done（2026-09-12）：impl-404；重放/透传/非终态/流式重放用例绿。
