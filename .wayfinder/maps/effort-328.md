# Wayfinder Map — Buzhou 干跑计划 JSONL 导出（effort #328，C 会话第 29 轮）

> C 会话第 29 轮。323 落了计划面（plan() 内存视图）+ Out of scope 承诺
> "持久化导出 export 族可后接"——兑现。计划单是给人审的：JSONL 一行一
> 调用，接进 317 ExportBundle 即成一揽子审阅包。

## Destination

`DryRunPlanJsonl`（core.exec，导出族新员——ModelCostLedgerJsonl 同形）：
`export(DryRunHook, Writer)` → 一行一 PlannedCall（toolCallId/tool/args——
args 以字符串快照列写入：计划单是人审口径非机器回放，不可序列化值不炸
导出）；空计划零行诚实；尾行 meta（dropped 计数）。

## Notes

- 号段：spec 328 / T647–T648 / impl-351。
- 借鉴：导出族既有配方（平铺 JSONL + 空表零行 + 人读/机读双口径）。

## Decisions so far

- args 字符串快照列（String.valueOf）——ObservabilityJsonlExporter "不可
  序列化降级"同哲学，但计划单整体降级为字符串口径（回放另立项）。
- dropped>0 时追加 {"meta":true,"dropped":N} 尾行——截断诚实可见。

## Out of scope

- 机器可回放格式（args 结构化序列化）；自动接 ExportBundle（宿主一行
  组包——317 已有 API）。

## Tickets

- [x] [T647 DryRunPlanJsonl](../tickets/T647-plan-jsonl.md)（impl-351）
- [x] [T648 回归与收口](../tickets/T648-plan-jsonl-close.md)（impl-351）
