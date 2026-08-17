---
Type: task
Status: closed
---
## Question

webhook 死信重放（T127 runbook 遗留「按 eventId 在消费端补录」人工口径）：框架是否提供重放 API？

## Resolution

AFK 自决：提供。`WebhookEventForwarder.replayDeadLetters()`：把全部死信迁回 outbox（attempts=0、立即可投递）并触发 dispatcher；返回重放条数；投递语义回到常规（可能再死信——幂等）。runbook §2 处置改为「replayDeadLetters() 一键重放」。产 spec 37 §B + impl-106。
