---
Type: task
Status: open
---
## Question

webhook outbox（min(1s×2^attempts,60s)）与 policy 轮询退避加 ±25% 随机抖动（ResilienceAdvisor jitter 同语义收窄）；确定性测试用注入 Random。验证：抖动域单测。
