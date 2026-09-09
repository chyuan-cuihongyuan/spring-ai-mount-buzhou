# Spec 328 — 干跑计划 JSONL 导出（effort #328）

> wayfinder map：`.wayfinder/maps/effort-328.md`（T647–T648）。兑现 323 的导出
> 后接承诺；导出族配方（ModelCostLedgerJsonl 同形）。

## Problem Statement

323 的计划面只在内存（`plan()` 视图）：演练窗口一清（clearPlan/重启）计划
单就没了——人工审阅、留档、跨团队流转都需要计划单落成文件。

## Solution

`DryRunPlanJsonl`（core.exec 静态导出器，导出族新员）：

- `export(DryRunHook hook, Writer out)` → 一行一 `PlannedCall`
  （`{"toolCallId","tool","args"}`）；返回行数。
- args 以字符串快照列写入（`String.valueOf`）——计划单是<b>人审</b>口径
  非机器回放；不可序列化值不炸导出（Observability 降级哲学的整体化）。
- 空计划零行诚实；dropped > 0 时追加 `{"meta":true,"dropped":N}` 尾行
  ——截断可见。

## User Stories

1. 作为运维，演练窗口结束我想把计划单落盘 JSONL 交给业务方审，所以
   不依赖窗口存活。
2. 作为运维，计划被截断（>100）时我想看到截断了多少，所以审阅时知道
   单子不全。
3. 作为宿主，我想把计划 JSONL 与观测报表组进同一个 ExportBundle，所以
   一份 ZIP 全审。

## Implementation Decisions

- 静态工具类（导出族同形——无状态无装配）。
- Jackson Streaming（JsonGenerator）逐行 flush——大计划不驻内存翻倍。

## Testing Decisions

- `DryRunPlanJsonlTest`：逐行字段对账/顺序即拦截序/空计划零行/
  dropped 尾行/不可序列化 args 不炸（字符串降级）。

## Out of Scope

- 机器可回放结构化 args；自动组包 ExportBundle（宿主 API 已有）。

## Further Notes

- 演练族闭环：干跑拦截（323）→ 计划面 → **计划落盘（本轮）**。
