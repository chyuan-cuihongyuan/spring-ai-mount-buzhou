# 146 — 模型池配额全候选执行

**Parent:** spec 49 §B / [T177](../tickets/T177-model-pool-quota.md)

**Status:** done

- [x] ResilienceAdvisor 候选级限流闸（fallbackOrRethrow/degradeFromCanary/金丝雀目标三路；拒绝→跳级不入熔断窗）
- [x] 候选成功后按实际 usage 记账 TPM（缺失留痕沿既有口径）
- [x] ModelRateLimiter.remainingRatio 探针 + 模块按已知模型名注册 ratelimit.remaining gauge
- [x] 测试：RPM=1 确定性主桶拒绝 + 候选过闸（拒绝族计数可达）/ remaining 探针 / 无限流回归零变化
