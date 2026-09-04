# Wayfinder Map — Buzhou 告警面板端点（effort #345，C 会话第 46 轮）

> C 会话第 46 轮。312 立规则触发、330 立静默/抑制——但运行时状态没有
> 面板面：「现在哪些规则 firing、哪些静默窗在吞、根因遮蔽了谁」要靠
> 翻日志。Grafana/Alertmanager UI 的思想：通知治理的状态面要可看。

## Destination

`BuzhouAlertsEndpoint`（/actuator/buzhou-alerts）：聚合 AlertRuleEngine
（firingView——引擎补只读视图）+ AlertGate（activeSilences/firingMechanisms/
inhibitRules）→ 一屏告警态；bean 缺席段为空（未配告警零变化）；挂
332/343 同 actuator 条件配置类。

## Notes

- 号段：spec 345 / T681–T682 / impl-368。
- 借鉴源：Alertmanager UI / Grafana alerting 面板。
- 纪律：只读；无引擎/门时空段诚实（不臆造）。

## Decisions so far

- 引擎补 firingView()（firing map 只读副本——方法级增量不增型）。

## Out of scope

- 写操作（静默按钮在 AlertGate API 已有）；历史/统计（计数器归 metrics）。

## Tickets

- [x] [T681 引警 firingView + 端点聚合](tickets/T681-alerts-endpoint.md)
- [x] [T682 装配 + 收口](tickets/T682-alerts-assembly.md)
