# Wayfinder Map — Buzhou 重试预算（effort #133，A 会话第 28 轮）

> A 侧票号 T501+ / spec 偶数段沿用。借鉴 Twitter Finagle retry budget。

## Destination

重试配额随流量百分比持续累积、重试支取、不足即拒——上游故障时重试量自动
被压到流量占比内（防重试风暴），恢复后自动回填无需窗口。

## Notes

- 毫单位内部整数；初始底数覆盖冷启动；denied 计数 = 风暴压制证据面；
  refill 运维逃逸。

## Decisions so far

- [RetryBudget](../tickets/T535-retry-budget.md) — deposit/tryAcquire/balance/
  denied/refill。

## Not yet specified

- 接线（模型重试路径/工具重试路径）；per-model 分账。

## Out of scope

- 窗口化语义（刻意——连续累积正是要义）；异步预算。

## Tickets

- [x] [T535 重试预算](../tickets/T535-retry-budget.md)（impl-300）
- [x] [T536 收口提交](../tickets/T536-retry-budget-close.md)（impl-300）
