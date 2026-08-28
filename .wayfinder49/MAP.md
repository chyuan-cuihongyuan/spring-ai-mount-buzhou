# Wayfinder Map — Buzhou eval run JSONL 导出（effort #49，50 轮自迭代第 14 轮）

> effort #49，延续 #48（T335–T336 / impl-234）。主线：观测数据有 OLAP JSONL 导出
> （spec 60/67），评估 run 没有——质量数据与行为数据无法在 DuckDB/ClickHouse 里
> 按 runId/时间轴 join 做联合分析（「哪类失败集中在哪个工具慢的会话」答不了）。

## Destination

`EvalRunJsonlExporter`（构造 EvalQueryService）：`exportRun(runId, Writer)`（item
行 ×N + summary 行 ×1，汇总列反规范化进每行——单表分析免 join；datasetFingerprint
列随 spec 82）；`exportAll(Writer)`（startedAt 倒序）；Jackson JsonGenerator 转义
纪律（一行一记录）；未知 runId = runFound=false 零行（诚实：不产出空 summary）。

## Notes

- 借鉴：观测导出 spec 60/67 同族（Langfuse/Helicone 摄取面思想）。

## Decisions so far

- 反规范化汇总列（OLAP 单表即可分析——join 留给跨域场景）。
- item 行带 actualPreview（eval 数据宿主自控；观测导出的合规口径不适用于此）。

## Not yet specified

- 增量水位（exportAllSince——eval run 无流式增量语义，需求证据后议）；gzip 压缩面。

## Out of scope

- 沿用 #7–#48；AB run 的 JSONL 导出（同构扩展，fog 记账）。

## Tickets

- [x] [T339 EvalRunJsonlExporter + 反规范化行形态](tickets/T339-eval-jsonl.md)（impl-235）
- [x] [T340 3 例红队（行数/JSON 合法/未知诚实/exportAll 倒序）+ 收口](tickets/T340-eval-jsonl-close.md)
