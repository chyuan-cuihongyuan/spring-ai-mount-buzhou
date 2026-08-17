# 59 — per-session 配额 + 限流器进程级修正（T84 决策落地）

**What to build:** resilience 新 `quota` 包 `SessionQuotaHook`（三维度/UTC 日窗/单键读时重置）+ ResilienceProperties 增 `SessionQuota` 组 + ModelRateLimiter 移到 configure() 进程级创建 + RuntimeConfig.merge 贡献 hook + stats/指标/事件。

**Blocked by:** 58（已 done）。

**Status:** done

- [ ] `SessionQuota` 配置组（turns/tool-calls/tokens-per-day，null=不限，负值 fail-fast）
- [ ] `SessionQuotaHook`：beforeTurn/beforeTool/beforeModel 三拦截点 + afterModel tokens 日键累计；单键 `epochDay:count` 读时重置；quota.exceeded 事件 + block
- [ ] ModelRateLimiter 移入 configure()（进程级共享）；RateLimitEndToEndTest 回归核对
- [ ] `ResilienceModule.configure`：有配额维度时 RuntimeConfig.merge(hooks(...), assemblyCustomizers(...))
- [ ] ResilienceStats.quotaRejections + 指标 buzhou.resilience.quota-rejected
- [ ] 测试：turns-per-day 超限 block、tokens-per-day 超限、无配额零开销、限流器跨会话共享（rpm=2 下两会话合计第 3 次被拒）、fail-fast

## Done

验证：`./mvnw -pl buzhou-resilience clean test` 78/78 绿（新增 SessionQuotaEndToEndTest 5 用例：turns 日窗拦截/tokens 日窗按累计拦截/per-session 独立/限流器跨会话共享/无配额零开销）；RateLimitEndToEndTest 随全量回归绿；starter 5/5、examples 62/62 绿。
落地：`quota/SessionQuotaHook`（order 1150；beforeTurn/beforeTool/beforeModel 三拦截点 + afterModel tokens 日键累计；单键 `epochDay:count` 读时重置）；SessionQuota 配置组（三维度 fail-fast）；ModelRateLimiter 移入 configure() 进程级创建（修正 N 会话=N 倍限额语义缺陷）+ acquireOrThrow/recordUsage 增按调用 emitter 重载 + RateLimitAdvisor 持会话通道 4 参构造；RuntimeConfig.merge 贡献 quota hook（有维度才挂）；ResilienceStats.quotaRejections + quota.exceeded 事件。
