---
Type: task
Status: closed
---
## Question

CanaryToolCallback（weight% 分流 + per 臂成败计数 + 劣化一次性粘性自动
回滚 + View 观测 + 同名校验 + 概率源可注入）。

## Resolution

done（2026-09-02）：impl-347；七用例绿（同错误率用例曾误配 weight=100
致 stable 零样本——改交替概率源混合流量后过）。
