# effort #700 — 能力门决策审计读数

- 会话：G 会话 700 系第 1 轮 ｜ spec [700](../../../docs/spec/700-capability-decision-audit.md) ｜ 票 [T1000](../tickets/T1000-capability-decision-audit.md)/[T1001](../tickets/T1001-capability-decision-audit-verify.md) ｜ impl600
- 借鉴：OpenPolicyAgent（OPA）Decision Logs——策略每个 allow/deny 决定结构化留痕+聚合读数（github.com/open-policy-agent/opa ≈10K star）

## 勘察（排重）

- 502 能力门 `CapabilityGateAdvisor`：拒绝仅以 `BuzhouException` 异常形态存在，无读数面。
- `CanaryToolCallback` 是工具金丝雀（诱饵工具），非决策审计——不同族。
- grep `DecisionLog|AuditLog`：guard 有 AuditChain（hash 链完整性族），resilience/capability 无审计。

## 决定

`CapabilityDecisionAudit`（resilience.capability）：deny 逐条环形留痕（容量 64，超出 dropped 计数）+ admit 只计数（量级大，诚实折中）+ per-model deny 聚合 + snapshot() 不可变报告。装配随 CapabilityPresentCondition 同条件；advisor 加 3 参构造（2 参委托 null=零审计向后兼容）。

## 测试

deny 留痕+聚合/环形覆盖 dropped/null fail-fast+不可变/advisor 接线冒烟。

## 诚实边界

admit 不逐条（总量大）；无持久化（内存有界，进程重启清零——审计连续性归日志/链族）；不改拒绝行为（纯旁路读数）。