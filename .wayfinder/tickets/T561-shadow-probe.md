---
Type: task
Status: closed
---
## Question

确定性采样 + 异步旁路对照 + 分歧样本环形。

## Resolution

done（2026-08-30）：impl-308；ShadowProbe（sha256 采样 + submit 即忘异常吞 +
agreed/diverged/sampled/error 计数 + 环形 32 样本）。
