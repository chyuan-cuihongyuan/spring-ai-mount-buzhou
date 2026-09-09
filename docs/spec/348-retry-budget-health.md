# Spec 348 — 重试预算健康面（effort #348）

> wayfinder map：`.wayfinder/maps/effort-348.md`（T687–T688）。C 会话第 49 轮。

## Problem Statement

重试预算（302）默默拦截重试风暴：denied 增长 = 保护生效，但聚合健康
面（/actuator/buzhou）看不到预算余量与拦截量——背压族四成员中唯独
预算无观测面。

## Solution

`RetryBudgetHealth`（core.health，机制名 `retry-budget`）：

- **恒 UP**：预算拦截是设计而非事故（诚实口径——DOWN 会误导重启/
  摘流量决策；342 探针分层下自然归 readiness 类但永不 DOWN）。
- **details**：{balance（余量）, withdrawn（已取）, denied（被拦）,
  minBalance}——RetryBudgetHolder 全局预算快照；denied 持续增长 =
  重试风暴被挡住的可见信号。
- 无全局预算（RetryBudgetHolder 空——未启用重试预算机制）→ UNKNOWN。
- 装配挂 core（BuzhouHealth 自动聚合 → /actuator/buzhou、312 告警
  源、332 探针、345 无涉）。

## User Stories

1. 作为运维，我想看到重试预算余量与被拦量，所以 重试风暴发生时
   「预算在挡」可见而非靠猜。
2. 作为 SRE，我想该面永不 DOWN，所以 保护生效不被误判为故障。
3. 作为使用者，未启用重试预算时面 UNKNOWN，所以 降级运行不误报。

## Implementation Decisions

- RetryBudgetHolder 查全局预算（null → UNKNOWN）；只读快照。

## Testing Decisions

- 有预算：UP + details 数值（deposit/tryAcquire 驱动）；
- 无预算：UNKNOWN；
- 装配：无 holder 内容时不红。

## Out of Scope

- denied 阈值告警（窗口语义另议）；per-model 分域预算。

## Further Notes

- 新公共类型 `RetryBudgetHealth` 随轮 regenerate 快照。
