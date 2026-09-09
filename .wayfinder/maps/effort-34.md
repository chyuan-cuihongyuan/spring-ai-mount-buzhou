# Wayfinder Map — Buzhou A/B run 落盘与查询（effort #34）

> effort #34，延续 #5–#33（累计 180 轮 / T1–T298 / impl 1–219）。
> 主线：**#31 fog 毕业生**——PairwiseEvalRunner 只返回内存结果；run 记录不落盘无法
> 回溯。对齐 EvalRunner 落盘形态（eval 合成会话 + 键前缀 + 查询面）。

## Destination

3 参构造（SessionStateStore 可空）：run 记录落 `ab.run.<runId>` 键（JSON：summary +
per-item verdict/error）；`abRuns(store, dataset?)` 摘要查询（startedAt 倒序）；不落盘
构造行为与 #31 一致；零新键。

## Notes

- 复用 EvalRunner.encode/decodeMap（包内静态面）；查询不触写路径。

## Decisions so far

- 摘要行不带 items 明细（明细在 JSON 内可审计；API 摘要轻量）。

## Not yet specified

- A/B run 完成事件（eval.run.completed 家族扩展——需求证据后议）。

## Out of scope

- 沿用 #7–#33；事件面；新配置键。

## Tickets

- [x] [T299 run 落盘 + AbRunSummary 查询面](../tickets/T299-ab-persist.md)（impl-220）
- [x] [T300 红队（落盘回读/过滤/倒序/不落盘零变化）+ 文档 + verify + 收口](../tickets/T300-ab-close.md)
