# Spec 94 — A/B run JSONL 导出（effort #55）

> wayfinder map：`.wayfinder/maps/effort-55.md`（T355–T356）。spec 88 fog 项收口（AB 面同构）。

## Problem Statement

eval run 可 OLAP JSONL 导出（spec 88），A/B verdict 明细不能：对比结论与
质量/行为数据在 OLAP 侧缺一角——「A/B 胜负与该数据集的失败项是否相关」这类
三表联合分析做不了。

## Solution

`AbRunJsonlExporter`（静态面）：`exportRun(store, runId, out)`——item 行
（kind=item：itemId/winner/reason/error verdict 面 + 汇总列反规范化 winRateA/
winRateB/winsA/winsB/ties/errors/total + datasetFingerprint + 时间轴）+ summary 行
×1；`exportAll(store, out)` startedAt 倒序。Jackson JsonGenerator 转义纪律；
未知 runId = runFound=false 零行。输出原文不落盘（spec 74）故无 actual 列。

## User Stories

1. 作为数据作者，我要 verdict 行进 OLAP，所以对比结论可与 eval/观测表 join。
2. 作为红队，我要未知 run 诚实零行，所以导出不误导。
3. 作为分析者，我要汇总列在每行，所以单表即可按数据集版本聚合胜率。

## Implementation Decisions

- 静态面（AB 查询本就是静态方法——不重复构造链）。
- 独立类（列形态与 eval item 行不同——verdict 面无 detail/duration/actual）。

## Testing Decisions

- verdict 行字段（WINNER_A/汇总列/指纹）+ summary 行；未知 runId 零行；
  exportAll 倒序覆盖。

## Out of Scope

- 输出原文落盘；gzip；分析 SQL 样例（runbook 另补）。

## Further Notes

- 与 spec 88/60/67 组成四导出面：观测 span/event、eval run、ab run——同一
  runId/时间轴语义。
