# Wayfinder Map — Buzhou 模型成本健康面（effort #139，A 会话第 34 轮）

> A 侧票号 T501+ / spec 偶数段沿用。

## Destination

/actuator/buzhou model-cost 段：在册模型 + 总成本双口径 + top-8 烧钱行
——「哪个模型在烧钱」一屏定位。

## Notes

- 恒 UP 观测（裁决在预算闸）；台账全局恒在（spec 176 自动入账）——无
  disabled 态；top-8 有界。

## Decisions so far

- [ModelCostHealth](tickets/T551-cost-health.md) — 全局台账直读。

## Not yet specified

- autoconfig 端点装配（bean 登记）。

## Out of scope

- DOWN 判定；价目快照。

## Tickets

- [x] [T551 成本健康面](tickets/T551-cost-health.md)（impl-306）
- [x] [T552 收口提交](tickets/T552-cost-health-close.md)（impl-306）
