---
Type: task
Status: closed
---
## Question

BuzhouRunawayProperties/BuzhouBackpressureProperties 零 fail-fast 校验；BuzhouWebhookProperties maxAttempts/outboxCapacity 非法值静默回退默认：校验补全面如何定？静默回退改显式抛是否入 pre-1.0 破坏性变更档？

## Resolution

AFK 自决：补全——runaway/backpressure 全键构造期 fail-fast（正整数/正时长/策略词封闭枚举/比例区间；
null=不限语义保留）；webhook max-attempts/outbox-capacity 静默回退改显式拒绝（pre-1.0 破坏性变更，
api-surface 入档）。JSR-303 注解化出界（沿用构造器校验路线）。产 spec 43 §B + impl-129。
