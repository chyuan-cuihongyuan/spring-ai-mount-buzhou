# 1515 — 降级存储契约对齐 + 机制计数口径统一

> 来源：M 会话第 17 轮 = effort #1515（impl 1118）。design-incompleteness 六-1 闭环 + 七-1 闭环。

## 背景

- 六-1：DegradingObservabilityStore 在 jdbc/redis 各一份且分叉——jdbc 版有 `buzhou.store.write.failures{policy=degrade}` 指标，redis 版缺失：同名降级策略两库观测行为不一致。
- 七-1：README「九大机制」vs CLAUDE.md「十大机制」（韧性层入列）——叙事 framing 矛盾。

## 目标

- redis 版 runDegradable 补同款指标（对齐 jdbc 先例 impl-41 / spec 13 §T66）；
- README 升十大机制：标题/正文三处措辞 + 表加第 10 行模型韧性层（buzhou-resilience）。

## 兼容性

指标新增（观测面补齐）；文档口径统一零行为变化。
