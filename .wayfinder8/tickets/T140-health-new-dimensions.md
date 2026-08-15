---
Type: task
Status: open
---
## Question

健康端点新维度：BuzhouHealth 详情未含 outbox 待决/死信水位、索引装配状态。是否补？

## Resolution

AFK 自决：补。core `WebhookOutboxHealth`（实现 BuzhouHealth：UP + details {pending, deadLetters, capacity}；forwarder 装配时才有）+ `SessionIndexHealth`（details {wired: true, rows 采样}——索引 store 有 get 无 count，rows 用 list 首页 size 近似或 details 只报 wired/降级态）。auto-config 注册两 bean。产 spec 39 §C + impl-113。
