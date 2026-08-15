---
Type: task
Status: open
---
## Question

resilience 模块按 modelName 池级 TPM/RPM 滚动窗口配额（与熔断同桶粒度）；超限快速失败（MODEL_QUOTA_EXCEEDED 可回退降级链）；remaining gauge 可观测；Clock 注入口径复用。验证：窗口滚动单测（注入时钟零真实等待）+ 回退联动断言。
