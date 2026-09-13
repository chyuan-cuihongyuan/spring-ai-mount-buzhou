# effort #740 — 供应商限流信号 stats 接线（719 扩散）

- 会话：G 会话 700 系第 41 轮 ｜ spec [740](../../../docs/spec/740-provider-signals-stats.md) ｜ 票 [T1082](../tickets/T1082-provider-signals-stats.md)/[T1083](../tickets/T1083-provider-signals-stats-verify.md) ｜ impl640
- 借鉴：—（719 信号的 stats 聚合接线）

## 勘察（排重）

- 719 parseFlexible 是纯解析——解析结果无处聚合（消费端拿到后各存各的）；ResilienceStats details 无 providerUtilization 字段（grep 零命中）。

## 决定

ResilienceStats 加 `updateProviderUtilization(double)`/`lastProviderUtilization()`（NaN 起始）+details 条件出现（有信号才出现——不污染默认读数）。消费端（advisor 拦截响应头调 parse 后）回写聚合。

## 测试

NaN 起始/details 条件出现/更新回落。

## 诚实边界
单值快照（最近一次）非时间序列；解析仍由消费端调用 719 原语。
