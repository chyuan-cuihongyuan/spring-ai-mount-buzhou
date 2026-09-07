# Wayfinder Map — Buzhou A/B run JSONL 导出（effort #55，50 轮自迭代第 20 轮）

> effort #55，延续 #54（T351–T352 / impl-240）。主线：**spec 88 fog 项「AB run
> 导出同构扩展」**——eval run 可进 OLAP，A/B verdict 面不能，三表联合分析缺一角。

## Destination

`AbRunJsonlExporter`（静态面——AB 查询本就是静态方法不重复构造链）：
`exportRun(store, runId, out)`（item 行 = verdict 面 winner/reason/error + 汇总列
反规范化 + 指纹列；summary 行 ×1）+ `exportAll(store, out)`（倒序）；Jackson
转义纪律；未知 runId = runFound=false 零行。

## Notes

- 借鉴：spec 88 同构（Langfuse/Helicone 摄取面）；输出原文不落盘故无 actual 列。

## Decisions so far

- 静态面（与 PairwiseEvalRunner.abRun/abRuns 同构）；独立类而非并入
  EvalRunJsonlExporter（列形态不同——verdict 面无 detail/duration/actual）。

## Not yet specified

- 三表 join 的分析 SQL 样例（runbook 补）；gzip。

## Out of scope

- 沿用 #7–#54；输出原文落盘（spec 74 决策不动）。

## Tickets

- [x] [T355 AbRunJsonlExporter 静态面](../tickets/T357-ab-jsonl.md)（impl-241）
- [x] [T356 3 例红队（verdict 行/未知诚实/倒序全量）+ 收口](../tickets/T358-ab-jsonl-close.md)
