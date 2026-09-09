---
Type: task
Status: closed
---
## Question

AgentBulkhead + 接线 + autoconfig。

## Resolution

done（2026-08-29）：impl-231；NOOP 零开销默认；三入口（chat/entity/stream doFinally）；
buzhou.bulkhead.* 三键默认关。
