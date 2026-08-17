# 37 — 横切 · 保留策略族 + RetentionSweeper + 触发公式

**What to build:** 「只进不出」终结：后台 RetentionSweeper 按声明式策略低频兑现——会话保留（锚点 closedAt、默认 PT72H、改短不追溯）、观测 TTL（PT7D 批删）、摘要版本修剪（保留 K=3）、ToolCallLog 窗口（PT7D）、RunRegistry COMPLETED 窗口（PT24H）；触发按 MaintenanceTrigger 公式（base+比例+封顶+硬兜底）。

**Blocked by:** 35（cleaner 基建）、36（上限语义）

**Status:** ready-for-agent

- [ ] SessionHistoryPolicy / ObservabilityTtl / MaintenanceTrigger 值对象 + SPI prune(policy)
- [ ] RetentionSweeper 后台执行器（默认 PT1H 可关；关闭时各策略仍可手动触发）
- [ ] 各 store prune 实现（批量限删 LIMIT；会话锚点=closedAt；改短不追溯）
- [ ] 摘要修剪 / ToolCallLog 窗口 / RunRegistry COMPLETED 窗口接入 sweeper
- [ ] 清理动作发事件（可观测）+ 失败 WARN 不中断
- [ ] 契约/单测：封闭会话到期清理、活动会话永不清、触发公式边界（小表兜底/大表封顶/硬性 floor）
