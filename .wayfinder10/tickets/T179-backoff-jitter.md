---
Type: task
Status: closed
---
## Question

webhook outbox（min(1s×2^attempts,60s)）与 policy 轮询退避加 ±25% 随机抖动（ResilienceAdvisor jitter 同语义收窄）；确定性测试用注入 Random。验证：抖动域单测。

## Resolution

spec 50 §B / impl-148 落地：webhook outbox 退避与 policy 轮询失败退避统一加 ±25% 抖动
（[0.75, 1.25]×base；防多实例同相位雷鸣羊群）；模型重试链 jitter（既有 ±0.5 归一口径）不动。
WebhookEventForwarder.jitteredBackoffMillis 静态 seam + DoubleSupplier 确定性注入测试
（0.0/0.5/1.0 三点钉边界；封顶 60s 抖动后 75s 不破）。core 331 全绿。
