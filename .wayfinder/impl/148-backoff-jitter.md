# 148 — 退避 jitter 补全

**Parent:** spec 50 §B / [T179](../tickets/T179-backoff-jitter.md)

**Status:** done

- [x] WebhookEventForwarder 退避 ±25% 抖动（jitteredBackoffMillis 静态可测 seam；DoubleSupplier 注入）
- [x] DbPolicyConfigProvider 轮询失败退避同口径
- [x] 测试：确定性随机源 0.0/0.5/1.0 三点钉边界（0.75×/1×/1.25×base）+ 封顶 60s 不破
- [x] core 331 全绿（含既有 outbox 回归）
