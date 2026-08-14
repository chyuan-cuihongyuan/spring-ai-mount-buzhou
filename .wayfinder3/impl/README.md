# wayfinder3 实现纵切片索引（effort #3「生产级收口」）

来源：spec 13（/to-tickets 切出）。编号续 effort #2（01–27）从 28 起。状态：`ready-for-agent` → `done`。

| # | 切片 | Blocked by | 状态 |
|---|------|-----------|------|
| 28 | core · Turn Deadline 贯穿 + 挂起免疫 + 故障注入构件 | — | done |
| 29 | 横切 · 异常分类体系 + 错误码 + 日志基线 | — | done |
| 30 | core · 优雅停机与生命周期 | 28 | done |
| 31 | stores · Schema 版本化迁移 + MySQL 幂等 + 恢复设施装配 | — | done |
| 32 | stores · 事务接线 + 并发正确性 + 降级语义 | 31 | done |
| 33 | core · 租约续租 + LeaseLost + 写路径 fence | 28, 29 | done |
| 34 | core · 事件背压 + 线程卫生 | 29 | done |
| 35 | 横切 · deleteSession 级联清理 + SessionCleaner | 31 | done |
| 36 | stores · InMemory 有界化 + 容量配额 | 29, 35 | done |
| 37 | 横切 · 保留策略族 + RetentionSweeper + 触发公式 | 35, 36 | done |
| 38 | memory+spill · 增长治理 + embedding 缓存 + 后台任务治理 | 37, 29 | done |
| 39 | guard · 审计链持久化 + 密钥版本化轮换 + 独立校验 | 29 | done |
| 40 | guard · policy 热加载 + 沙箱限额 | 39 | done |
| 41 | 横切 · 泄漏检测 + 健康检查 + 指标 | 29, 34, 40 | done |
| 42 | 横切 · 配置全参数化 + 启动校验 + FailureAnalyzer + 默认值安全化 | 41 | ready |
| 43 | 收口 · 配置元数据 + 韧性矩阵补齐 + 终验 | 42 | ready |
