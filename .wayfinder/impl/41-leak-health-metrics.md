# 41 — 横切 · 泄漏检测 + 健康检查 + 指标

**What to build:** 泄漏可发现、健康可探测、行为可量化：ResourceLeakDetector（四级采样 + 出租时长阈值 + LeakListener）挂会话资源/句柄/租约；每机制 HealthIndicator（禁用报 UNKNOWN）+ 只读 @Endpoint(id="buzhou") 快照；每机制 MeterBinder（@ConditionalOnClass，未装 micrometer 时 no-op）；指标命名 buzhou.<mech>.<测量>、tag 值有界。

**Blocked by:** 29（分类）、34（事件计数）、40（guard 面就绪）

**Status:** ready-for-agent

- [ ] ResourceLeakDetector（DISABLED/SIMPLE 默认/ADVANCED/PARANOID、1/128 采样、出租阈值、LeakListener）挂三处
- [ ] 内部 BuzhouMetrics recorder（无 micrometer 时 no-op）
- [ ] HealthIndicator×3（memory/spill/guard：DOWN 仅当核心职能不可用）+ @Endpoint(id="buzhou") 只读快照
- [ ] MeterBinder conditionalOnClass；指标集：turn.duration(timer,outcome)/tool.calls(counter,outcome)/eventbus.dropped/compaction/spill.requests/guard.checks/store.write.failures
- [ ] ApplicationContextRunner：有/无 micrometer × enabled/disabled 装配矩阵
- [ ] 单测：泄漏检测报告未释放资源、健康聚合不误 DOWN
