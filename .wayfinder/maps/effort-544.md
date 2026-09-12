# Wayfinder Map — Buzhou 评估 run 项耗时分布（effort #544，E 会话第 44 轮）

> E 会话第 44 轮（108/111 timer 面的分布化扩散轮；416 分位族同法）。
> 勘察：run 总时长 timer（111）+ 项 durationMs 字段——run 内「慢在哪
> 一项」的分布读数空白。

## Destination

`eval.EvalRunDurationStats`（纯函数）：analyze(run) → 项耗时 p50/p95
（exact 最近秩）+ max + 最慢 top3（降序+同值字典序稳定）；空 run null
诚实空值。

## Notes

- 号段：spec 544 / T841-842 → 实际 T843-844 已用；本票 T845-846 / impl-446。

## Out of scope

- 跨 run 趋势；JSONL 导出。

## Tickets

- [x] [T845 分布原语](../tickets/T845-run-duration-stats.md)
- [x] [T846 零样本与 fail-fast](../tickets/T846-run-duration-edge.md)
