---
Type: task
Status: closed
---
## Question

resilience 模块按 modelName 池级 TPM/RPM 滚动窗口配额（与熔断同桶粒度）；超限快速失败（MODEL_QUOTA_EXCEEDED 可回退降级链）；remaining gauge 可观测；Clock 注入口径复用。验证：窗口滚动单测（注入时钟零真实等待）+ 回退联动断言。

## Resolution

spec 49 §B / impl-146 落地：ResilienceAdvisor 增候选级限流闸（candidateLimiter，null=既有行为）——
fallback / 金丝雀回退 / 金丝雀目标三路候选统一 acquireOrThrow + 成功后按实际 usage 记账 TPM；
拒绝跳下一候选（计既有 rate-limit-rejected 族、不入熔断窗）。ModelRateLimiter 增 remainingRatio
探针 + 模块按已知模型名集（主/备/shadow）注册 buzhou.resilience.ratelimit.remaining{dimension} gauge。
**实现期诚实裁定**：外层 RateLimitAdvisor 每逻辑调用先于候选闸扣减（锁步结构），单 limiter 配置下
候选拒绝分支为防御性闸；且外层把降级服务响应 usage 记入主桶（既有逻辑记账语义）与候选级准确记账
双账并存——均入档 spec 49 §B 与测试注记。TPM 预检受连续 refill 影响不确定，拒绝确定性测试改用
RPM=1 整数令牌口径。3 新测试绿，resilience 99 全绿。
