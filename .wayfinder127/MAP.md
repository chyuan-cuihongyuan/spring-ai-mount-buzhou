# Wayfinder Map — Buzhou PII 命中报表导出（effort #127，A 会话第 22 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 164 fog「报表 JSONL 导出」收口。

## Destination

PiiHitStatsJsonl：合规命中排行平铺 JSONL——export → reset 循环即每窗口一份
合规报表（与错误签名/观测/评估导出族同管线）。

## Notes

- 与 top() 同序（count 降序 + 名字典序）；转义纪律（引号/换行不撕行）；
  空表零行诚实。

## Decisions so far

- [报表导出](tickets/T519-report-export.md) — 静态面 + 返回行数。

## Not yet specified

- gzip 面（导出族惯例——调用方可自行包流，与 spec136 组合）。

## Out of scope

- 命中样本明细（只有计数——原文不落表是纪律）。

## Tickets

- [x] [T519 报表导出](tickets/T519-report-export.md)（impl-294）
- [x] [T520 收口提交](tickets/T520-report-export-close.md)（impl-294）
