---
Type: grilling
Status: closed
blocked-by: T69
---

## Question

buzhou-resilience 的分支实现（Future-needs-to-be-supplemented，领先 main 15 commits，含重试/退避/错误分类/限流/deadline 取消 + 6 测试类）如何合入 main？决策点：merge vs 按规范重写落仓；合入后补什么（reactor/BOM/starter 装配、熔断是否本轮做、退避 jitter、日志/健康/指标/配置元数据对齐 core 四模块标准）；分支上未提交的本地改动痕迹如何处置。

## Resolution

**不合分支，按增量移植**。分支（15 commits/12K 行）从 impl-28 之前分叉，其优雅停机/事件背压与 main 的 impl-30/34 平行且语义陈旧，docs/spec 编号 10-14 与 main 冲突；`git merge` 不可行。真增量三块，全部移植到 main 现架构：
1. **buzhou-resilience 模块整块移植**（重试/退避/错误分类/超时取消/onModelError/流式 + RPM/TPM 双桶限流 + 6 测试类）——模块自包含（仅依赖 buzhou-core + spring-ai-client-chat），移植后对齐 core 生产级标准：日志基线、健康/指标、配置校验+元数据、BOM/starter/reactor 接线。
2. **core/runaway 失控检测移植**（RunawayHook/RunawayCounters/双窗口硬顶/软退出/确定性重复检测 + 691 行 e2e）——适配 main 当前 hook 链与 BuzhouCoreProperties 体系。
3. **core/backpressure SpawnGate 移植**（会话并发容量闸 + OverloadPolicy 两档 + SessionCapacityExceededException）——适配 main 的 session/生命周期 API。
**明确不移植**：分支的 crash-recovery（DurabilityTiered/DedupGate/LeaseHeartbeat——与 main 已落地的 RunRecoveryService/impl-30 语义重叠，且基于陈旧 core 内部结构）、分支的 graceful-shutdown（main impl-30 已覆盖）、`.scratch/` 过程文件、`docs/production-readiness/`（内容并入本轮 spec/研究）、分支 spec 10-14（有效内容并入 docs/spec/15-model-resilience.md 与 spec 14 对应节，编号避开 main 10-13）。分支处置：保留分支不删（历史参考），main 落地后在其上追加 `superseded-by-main` 注记 commit。熔断（circuit breaker）本轮不做（限流+重试+deadline 已覆盖主要面），注记开放问题。（可推翻）
