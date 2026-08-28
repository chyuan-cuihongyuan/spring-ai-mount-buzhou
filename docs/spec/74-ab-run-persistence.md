# Spec 74 — A/B run 落盘与查询（effort #34）

> wayfinder map：`.wayfinder34/MAP.md`（T299–T300）。#31 fog 毕业生。

## Problem Statement

PairwiseEvalRunner 结果只在内存：无法回溯历史 A/B 结论、无法跨重启对账。

## Solution

3 参构造注入 SessionStateStore：run 记录落 eval 合成会话（键 `ab.run.<runId>`，
JSON 含 summary 与 per-item verdict/error）；`abRuns(store, dataset?)` 摘要查询
（startedAt 倒序、dataset 可过滤）。不落盘构造行为不变；零新键。

## User Stories

1. 作为评估作者，我要 A/B 结论可回溯，所以选型决策有历史依据。
2. 作为红队，我要落盘-回读汇总等值，所以记录无漂移。
3. 作为既有用户，我要 2 参构造零变化，所以升级零风险。

## Implementation Decisions

- 复用 EvalRunner encode/decodeMap（同编解码面）；摘要行不带 items（明细留 JSON）。

## Testing Decisions

- 落盘回读汇总等值；dataset 过滤；倒序；不落盘构造零变化（既有 3 例回归）。

## Out of Scope

- 事件面；明细 API；新配置键。

## Further Notes

- 键前缀 ab.run. 与 eval.run. 同命名空间纪律（scanByPrefix 前缀互不干扰）。
