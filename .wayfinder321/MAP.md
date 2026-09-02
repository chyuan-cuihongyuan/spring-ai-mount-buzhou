# Wayfinder Map — Buzhou SLO 错误预算燃尽率（effort #321，C 会话第 22 轮）

> C 会话第 22 轮。工具成败已有两个消费面（熔断 131 跳闸 / 重试预算 302 限流），
> 都是"出了事马上反应"；缺 SRE 的预算视角：错误率对 SLO 的**燃尽率**——
> 不快不慢地告诉你"这个月的服务额度正在以几倍速烧"（Google SRE workbook
> multi-window burn rate 思想收窄为单窗+阈值，for 持续窗交给 312 告警引擎）。

## Destination

`ErrorBudget`（core.health：桶环窗错误率 + burn = errorRate/(1−SLO)，
min-samples 防噪，scope 256 封顶折叠）+ `ErrorBudgetHook`（order 250 纯观察
afterTool 喂成败——与熔断 hook 同标记语义）+ `ErrorBudgetHealth`
（mechanism "error-budget"，DOWN 严格语义 = 燃尽超阈 SLO 失守，样本不足
UNKNOWN）——接入 312 AlertRuleEngine（for 窗吸收瞬态）。

## Notes

- 号段：spec 321 / T633–T634 / impl-344。
- 借鉴：Google SRE 错误预算（alert on burn rate）；Prometheus record-rule
  同构（error_rate / (1 - slo)）。

## Decisions so far

- 单窗 + burn 阈值（默认 2×）+ min-samples（默认 20）：multi-window
  分层告警的宿主可用多 ErrorBudget 实例表达——库不预设告警分级。
- 成败判定复用 131 同语义（ToolFeedbackType.isErrorFeedback：执行失败 +
  校验失败均计败）。
- 未配 `buzhou.error-budget.slo` 不装配（零变化）。

## Out of scope

- multi-window 分层燃尽（14.4×/6× 双窗联判——宿主多实例表达）；预算按
  月配额（时间箱周期记账）；自动降载联动。

## Tickets

- [x] [T633 ErrorBudget + hook + 健康面 + 装配](tickets/T633-error-budget.md)（impl-344）
- [x] [T634 回归与收口](tickets/T634-error-budget-close.md)（impl-344）
