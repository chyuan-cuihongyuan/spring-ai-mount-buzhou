# Spec 76 — A/B run 明细查询（effort #37）

> wayfinder map：`.wayfinder37/MAP.md`（T305–T306）。#36 fog 毕业生。
> 借鉴：LangSmith run detail API（list + get 两级查询面）。

## Problem Statement

`abRuns` 摘要查询（spec 74）只有汇总行；per-item 裁决明细只在落盘 JSON 内，宿主
无查询入口。eval 侧已有 `EvalQueryService.run(runId)` 对称面，ab 侧缺失。

## Solution

`PairwiseEvalRunner.abRun(stateStore, runId)` → `Optional<PairwiseEvalResult>`：
verdict 面明细（itemId / winner / reason / error）+ summary 全量；输出原文不落盘
（spec 74 决策）故回读为 null。`mapToResult` 与 `abRuns` 共用解码底座；查询不触
写路径。

## User Stories

1. 作为评估作者，我要按 runId 取 per-item 裁决，所以单条争议可回放定位。
2. 作为红队，我要落盘-回读明细等值，所以解码无漂移。
3. 作为宿主，我要 ab./eval. 前缀隔离，所以两族 runId 互不误命中。

## Implementation Decisions

- 静态方法挂 PairwiseEvalRunner（与 abRuns 同居——eval 侧服务类的拆分在 A/B 面
  尚无第二个查询维度，不预建类）。
- error 项 verdict 为 null、win 项 error 为 null（落盘字段即真相）。

## Testing Decisions

- 1 win + 1 error 混合 run：winner/reason/error 逐字段等值；输出回读 null。
- 未知 runId = empty；eval runId 查 ab 面 = empty、ab runId 查 eval 面 = empty。

## Out of Scope

- 明细分页；输出原文落盘（schema 变更另议）。

## Further Notes

- 与 EvalQueryService.run 的差异：eval 明细含 actualPreview，ab 明细只有裁决面
  （A/B 输出体量大，落盘省空间是 spec 74 的既定取舍）。
