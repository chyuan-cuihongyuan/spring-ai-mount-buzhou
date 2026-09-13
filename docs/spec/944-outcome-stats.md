# 944 — 工具调用结局分布读面

> 来源：I 会话第 44 轮 = effort #944（[T1329](../../.wayfinder/tickets/T1329-outcome-stats-shape.md) / [T1332](../../.wayfinder/tickets/T1332-outcome-stats-verify.md) / impl 693）。spec 50 事件溯源工具日志的聚合读面——TIMEOUT 高占比=超时配置问题，CANCELLED=取消风暴，分布是根因分诊第一层。

## 目标

- recovery 包新公共纯函数类 `ToolCallOutcomeStats`：
  - `stats(List<ToolCallLogEntry> entries)`：四桶（COMPLETED/FAILED/TIMEOUT/CANCELLED）计数；
  - `record OutcomeStats(long completed, long failed, long timeouts, long cancelled)` + `total()` 守恒便捷；
  - entries null fail-fast；单条 null outcome 跳过（诚实计数口径）；
- 纯函数零 IO（922/936 纯函数纪律同款）。

## 兼容性

纯增量：新公共类型，零既有行为变化。
