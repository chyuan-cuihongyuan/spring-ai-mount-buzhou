# Wayfinder Map — Buzhou 配置漂移审计（effort #414，D 会话第 15 轮）

> D 会话第 15 轮。勘察（2026-09-08）：343 端点只给「当前生效配置」快照
> （带掩码）；320/325/340 各自热重读 yml 子集——**变更无审计**：谁在何时
> 把哪个旋钮从什么拧到什么，无事件、无留痕。ArgoCD 的 drift detection
> 思想（期望 vs 实际的 diff 可见）无对应物。

## Destination

`core.config.ConfigDriftAuditor`（ArgoCD drift detection 借鉴）：SmartLifecycle
周期快照 `buzhou.*` 全属性（EnumerablePropertySource 枚举——343 同法+
同款末段掩码）→ 与上轮 diff → 每处变更发事件 `config.changed`
{key, from, to}（值经掩码——敏感值不进事件流）+ 计数
`buzhou.config.changed`；基线在首拍建立（首拍零事件）。yml
`buzhou.config-audit.{enabled=false, interval=30s}`；独立调度与 405 同法。

## Notes

- 号段：spec 414 / T719–T720 / impl-387。
- 借鉴源：ArgoCD（25k★）drift detection——「实际跑的」与「以为在跑的」
  之差必须可见。
- 纪律：掩码与 343 同一判定（末段子串——宁掩勿漏）；轮询对账而非事件
  订阅（EnvironmentChangeEvent 是 spring-cloud 语义——plain boot 无此
  事件，轮询两栖）。

## Out of scope

- spring-cloud 事件订阅加速（有 cloud 上游时另议）；变更回滚；yml 源
  定位（哪个 propertySource 变了——快照面不含）；外部告警（312 族消费
  事件即可）。

## Tickets

- [x] [T719 ConfigDriftAuditor](../tickets/T719-config-drift-auditor.md)
- [x] [T720 掩码/基线/装配语义](../tickets/T720-config-drift-semantics.md)
