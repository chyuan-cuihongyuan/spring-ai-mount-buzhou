# Spec 170 — 技能使用报表导出（effort #129）

> wayfinder map：`.wayfinder129/MAP.md`（T524–T525）。spec 140 fog「导出半边」
> 收口。导出族第七员（观测/评估/A-B/错误签名/清单/PII 之后）。

## Problem Statement

SkillUsageStats 只有进程内 topUsed/unused 查询——技能热度榜的窗口时序分析
（本窗口 vs 上窗口谁上位）缺导出面。

## Solution

`skill/SkillUsageStatsJsonl.export(stats, writer)`：全部在册使用平铺一行一
JSON（`{"skill":...,"loads":N}`），与 topUsed 同序（count 降序 + 名字典序）；
返回行数。export → reset 循环 = 每窗口一份热度榜（spec 121 同纪律）。
空表零行诚实。

## User Stories

1. 作为平台运营，我把热度榜灌进 OLAP 按窗口对比，所以「新技能上位/老技能
   退场」有数据叙事。
2. 作为运维，export 后 reset 表永有界——与全导出族同一套 cron。

## Testing Decisions

- 红队：同序 + 行解析 + 列名；空表零行 + 窗口 reset 循环。

## Out of Scope

- unused 列扩展；gzip；样本留存。

## Further Notes

- 与 spec 116（搜索遥测）组合：搜索热度 + 加载热度 = 完整漏斗时序。
