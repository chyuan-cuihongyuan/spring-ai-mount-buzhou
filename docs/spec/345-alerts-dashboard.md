# Spec 345 — 告警面板端点（effort #345）

> wayfinder map：`.wayfinder/maps/effort-345.md`（T681–T682）。C 会话第 46 轮。

## Problem Statement

告警规则（312）与通知策略门（330）的状态没有运行时面板面：「现在哪
些规则在 firing、哪些静默窗在吞通知、抑制视图里根因遮蔽了谁」只能翻
日志重建——通知治理有控制面无状态面。

## Solution

`/actuator/buzhou-alerts`（BuzhouAlertsEndpoint，只读，Alertmanager UI/
Grafana alerting 面板思想）：

- **engine 段**：`AlertRuleEngine.firingView()`（新增只读视图——规则名
  → firing 布尔）+ rules 清单（name/mechanism/for）。
- **gate 段**：`activeSilences()`（在吞哪些机制、到几时）+
  `firingMechanisms()`（抑制判定依据）+ `inhibitRules()`。
- bean 缺席 → 对应段空 map/空 list 诚实（未配告警零变化）；挂 332/343
  同 actuator 条件配置类。

## User Stories

1. 作为值班人，我想一屏看到 firing 规则与生效静默窗，所以 「通知没来
   是没坏还是被吞」当场可答。
2. 作为运维，我想看抑制 firing 视图，所以 「衍生告警为何静默」可定位
   到根因机制。
3. 作为使用者，未配告警时端点返回空段，所以 诚实不臆造。

## Implementation Decisions

- firingView = 引擎 firing map 只读副本（规则名→是否 firing）。
- 端点构造接受 Optional 语义（null 容忍——ObjectProvider getIfAvailable）。

## Testing Decisions

- 端点：engine+gate 齐备全量段 / 部分缺席空段 / payload 键形。
- 装配：actuator 下有端点；引擎 firingView 单元（触发后见 true）。

## Out of Scope

- 写操作（静默/撤销按钮在 AlertGate 运行时 API）；告警历史统计。

## Further Notes

- 新公共类型 `BuzhouAlertsEndpoint` 随轮 regenerate 快照；
  firingView 方法级增量不增型。
