---
Type: task
Status: closed
---
## Question

租约式凭证：签发/续租/吊销/惰性过期。

## Resolution

done（2026-08-30）：impl-289；SecretLeases（Clock 注入 + 同名重签覆盖 +
过期续租拒绝 + 三计数 + activeLeases 名单不含值）。
