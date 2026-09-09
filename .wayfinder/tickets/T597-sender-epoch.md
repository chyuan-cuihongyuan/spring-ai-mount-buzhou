---
Type: task
Status: closed
---
## Question

发送侧持久纪元（meta.epoch，max(持久+1, 墙钟毫秒) 回写）+ 信封 epoch 字段。

## Resolution

done（2026-09-01）：impl-326；WebhookOutbox 构造期纪元初始化与回写（存储写
失败降级墙钟值），`epoch()` 只读暴露；WebhookEventForwarder 信封置 epoch。
