---
Type: task
Status: closed
---
## Question

事件多目的地扇出：N sink 独立投递语义 + outbox 隔离。

## Resolution

done（2026-08-30）：impl-288；WebhookFanout（广播 + 逐 sink include-types +
close 全关；零新投递逻辑——全部复用 forwarder）。
