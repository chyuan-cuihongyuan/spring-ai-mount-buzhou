---
Type: task
Status: closed
---
## Question

outbox 实时滞后读数：pending/最老积压 age（含退避中）/死信数。

## Resolution

done（2026-08-30）：impl-280；WebhookOutbox.pendingOldest（包内）+
WebhookOutboxLag（read/stalled/bindGauges；countByPrefix 下推 + Clock 注入）。
