# Spec 88 — eval run JSONL 导出（effort #49）

> wayfinder map：`.wayfinder/maps/effort-49.md`（T339–T340）。与观测导出 spec 60/67 同族。

## Problem Statement

观测数据可 OLAP JSONL 导出（spec 60/67），评估 run 不能：质量数据（passRate/失败
明细）与行为数据（span/事件）无法在 DuckDB/ClickHouse 按 runId/时间轴 join——
「质量波动是否集中在某类慢会话」这类联合分析做不了。

## Solution

`EvalRunJsonlExporter(EvalQueryService)`：`exportRun(runId, Writer)` 每 item 一行
（kind=item）+ summary 一行（kind=summary）；汇总列（passRate/passed/failed/
errored/total/datasetFingerprint/startedAt/finishedAt）反规范化进每行——单表分析
免 join；序列化走 Jackson JsonGenerator（换行天然转义）；未知 runId =
`runFound=false` 零行。`exportAll(Writer)` 按 startedAt 倒序全量。

## User Stories

1. 作为数据作者，我要 eval 行进 DuckDB，所以质量-行为联合 SQL 可写。
2. 作为红队，我要一行一记录转义纪律，所以坏 detail 不炸装载。
3. 作为排查者，我要未知 run 诚实零行，所以导出结果不误导。

## Implementation Decisions

- 反规范化汇总列（join 留给跨域；OLAP 列宽成本低）。
- item 行含 actualPreview（eval 数据宿主自控面——与观测导出合规口径区分）。

## Testing Decisions

- 行数 = items+1；逐行独立 JSON 可解析（含 detail 多行场景的转义纪律）；
  未知 runId 零行 + runFound=false；exportAll 倒序覆盖全部 run。

## Out of Scope

- AB run 导出（同构扩展）；增量水位；gzip。

## Further Notes

- 与 spec 82 指纹列配合：OLAP 侧可直接按数据集版本分组聚合。
