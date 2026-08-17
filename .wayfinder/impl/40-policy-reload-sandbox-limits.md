# 40 — guard · policy 热加载 + 沙箱限额

**What to build:** 策略变更不重启、沙箱资源有上限：PolicySource（classpath/file，etag=内容 hash）+ PolicyRefresher 轮询（PT30S 可关）快照原子替换，失败沿用旧版绝不部分生效，provenance（revision）进 PolicyDecision；PolicyGateHook 进自动装配；SandboxLimits（timeout/maxOutputBytes/可选 memory）+ CommandResult truncated/killedReason；Deno 探测缓存。

**Blocked by:** 39（guard 模块顺序协作）

**Status:** ready-for-agent

- [ ] PolicySource + PolicyRefresher（轮询、etag 条件拉取、失败沿用旧快照 + 指标）
- [ ] 快照 provenance（revision + activatedAt）写入 PolicyDecision
- [ ] PolicyGateHook 自动装配（buzhou.guard.policy.enabled）
- [ ] SandboxLimits 配置化（timeout/maxOutputBytes/memory 可选）；CommandResult 增 truncated + killedReason(timeout|memory|output|manual)
- [ ] DenoSandbox --version 探测结果缓存（TTL 重探）
- [ ] 单测：热更后新决策用新规则、坏规则文件不生效沿用旧版、输出超限截断显式标记、探测缓存生效
