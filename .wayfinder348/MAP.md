# Wayfinder Map — Buzhou 重试预算健康面（effort #348，C 会话第 49 轮）

> C 会话第 49 轮。302 落地的重试预算（Finagle retry budget）在默默
> 拦重试——denied 计数增长说明「重试风暴被预算挡住」（保护生效），
> 但健康面聚合里没有它：运维看不见预算余量与拦截量。背压族
> （123 闸/302 预算/335 冻结/342 cordon）唯独预算无观测成员。

## Destination

`RetryBudgetHealth`（机制名 retry-budget）：恒 UP（预算拦截是保护
生效不是故障——诚实口径），details = {balance, withdrawn, denied,
minBalance}（RetryBudgetHolder 全局预算快照）；无全局预算
（未启用重试预算）UNKNOWN。装配挂 core（holder 空 = 不装配或 UNKNOWN）。

## Notes

- 号段：spec 348 / T687–T688 / impl-371。
- 借鉴源：Finagle retry budget + Grafana 预算水位面板思想（观测成员轮）。
- 纪律：只读；DOWN 永不（保护性机制——被拦是设计而非事故，文档化）。

## Out of scope

- denied 阈值告警（需要窗口语义——312 族消费 details 可配 for 规则但
  恒 UP 不触发；真需求出现再议）；per-model 预算分域。

## Tickets

- [x] [T687 RetryBudgetHealth](tickets/T687-retry-budget-health.md)
- [x] [T688 装配 + 收口](tickets/T688-budget-health-assembly.md)
