# Wayfinder Map — Buzhou 成本健康段装配（effort #140，A 会话第 35 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 190 fog「bean 登记」收口。

## Destination

ModelCostHealth 进 autoconfig（@ConditionalOnMissingBean 兜底）——健康端点
恒有 model-cost 段。

## Notes

- 台账全局恒在 + 预算钩子自动入账（spec 176）→ 无条件 bean 合理（空台账
  诚实空态）。

## Decisions so far

- [装配](tickets/T554-cost-bean.md) — 一 bean + 启动校验回归。

## Tickets

- [x] [T554 成本段装配](tickets/T554-cost-bean.md)（impl-307）
- [x] [T555 收口提交](tickets/T555-cost-bean-close.md)（impl-307）
