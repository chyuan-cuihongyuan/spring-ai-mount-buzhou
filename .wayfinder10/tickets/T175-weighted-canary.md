---
Type: task
Status: open
---
## Question

降级链条目可配 weight（默认全 1 等价现状）；选路按 session 稳定哈希（同会话不漂移）；熔断/失败仍按序回退不受 weight 影响（weight 只影响健康候选的首选）。LiteLLM Router simple-shuffle 语义收窄为稳定加权。验证：权重分布单测 + 同会话稳定性断言。
