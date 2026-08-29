# Wayfinder Map — Buzhou 技能使用报表导出（effort #129，A 会话第 24 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 140 fog「export 半边」收口；
> 导出族第七员。

## Destination

SkillUsageStatsJsonl：使用排行平铺 JSONL——export → reset 每窗口一份热度榜。

## Notes

- 与 topUsed 同序；空表零行诚实；转义走 Jackson 管线。

## Decisions so far

- [报表导出](tickets/T524-skill-report.md) — 静态面 + 返回行数。

## Not yet specified

- unused 清单导出列（治理面扩展）。

## Out of scope

- gzip 面；样本留存。

## Tickets

- [x] [T524 技能报表导出](tickets/T524-skill-report.md)（impl-296）
- [x] [T525 收口提交](tickets/T525-skill-report-close.md)（impl-296）
