# 1025 — 内嵌策略引擎判定分布读面

> 来源：J 会话第 26 轮 = effort #1025（[T1501](../../.wayfinder/tickets/T1501-policy-engine-stats-shape.md) / [T1502](../../.wayfinder/tickets/T1502-policy-engine-stats-verify.md) / impl 778）。与 R1 同谱系（OPA 判定分布）落点 guard-24 内嵌引擎；四桶含 ESCALATE 人工通道细分。

## Problem Statement

`EmbeddedPolicyEngine.decide`（guard-24 OPA 子集：声明式规则 → allow/deny/escalate，默认拒，首条命中生效）零计数：allow/deny/escalate 判定分布、escalate 经人工审批转 allow 的通道压力不可见——策略调优（规则覆盖合理性、审批积压、默认拒误伤面）无据。

## 目标

- `EmbeddedPolicyEngine` 增量（buzhou-guard policy 包，实例级）：`allowCount` / `denyCount` / `escalateCount` / `escalateApprovedCount` 四 AtomicLong——守恒不变量**四桶和 == decide 调用数**（含默认拒与输入缺失拒绝路径）。
  - ESCALATE 规则 + 人工已审批 → 计 escalateApprovedCount（决策为 allow——FIDES approver 通道）；
  - ESCALATE 未审批 → escalateCount；
- 嵌套 record `PolicyDecisionStats(long allowCount, long denyCount, long escalateCount, long escalateApprovedCount)` + `stats()` 快照。
- 判定返回值（action/reason/revision）逐位不变。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按规则 id 分桶命中分布（规则数可控但留作后续——先验总体分布）。
- 决策耗时分布（hook timing 域）。
