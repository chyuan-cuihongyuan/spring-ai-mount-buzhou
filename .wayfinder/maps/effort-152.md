# Wayfinder Map — Buzhou 会话雾账本轮（effort #152，A 会话第 47 轮）

> 文档轮：收口前把 #86-#151 各轮「Not yet specified」滚动账本归一。

## Destination

全会话未收口 fog 的一页账（下会话种子）——按主题域分组，去重合并。

## 会话 fog 总账（下会话种子）

- **并发/背压**：RetryBudget 接线（模型/工具重试路径）；事务批回滚/补偿；
  舱表跨实例汇聚查询面。
- **预算/成本**：key 配额 Redis 共享后端；成本 per-tenant 拆列；价目快照随单。
- **观测**：按错误签名分层采样率；巡检犬/清理任务 autoconfig 锁路径键；
  PII 命中分侧列拆分；导出族 gzip 全员化 + 合流打包。
- **评估**：门禁结果入档随 run；期望套件持久化；CI 输出格式。
- **技能**：搜索→加载漏斗组合分析；嵌入缓存（渲染缓存第二消费方）。
- **基础**：租户存储键前缀（SessionStateStore/归档）；租户→key 配额路由；
  ConfigDoctor 自定义规则 SPI；咨询锁租约续期。
- **质量**：jqwik 引入评估（shrink 收益）；性质测试 III（导出族 round-trip）。

## Tickets

- [x] [T581 雾账归一](../tickets/T581-fog-ledger.md)（impl-319）
- [x] [T582 收口提交](../tickets/T582-fog-ledger-close.md)（impl-319）
