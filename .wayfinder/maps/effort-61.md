# Wayfinder Map — Buzhou 折入速率指标（effort #61，50 轮自迭代第 26 轮）

> effort #61，延续 #60（文档轮）。主线：**spec 95 fog 项「折入速率指标」**——
> trigger 溯源进了事件面，但指标面（counter）缺席：漂移触发率 vs 积压触发率
> 趋势、breaker 开路跳过量不可聚合。

## Destination

IVP 两 counter（经 BuzhouMetricsHolder，tag trigger 三值有界）：
`buzhou.memory.summary.folded`（折入成功——与 onSummaryFolded 通知同点）+
`buzhou.memory.summary.fold-skipped`（breaker 开路跳过——跳过也带 trigger，观测
「该折而未折」的堆积来源）。零新键零行为变化。

## Notes

- 借鉴：Micrometer tag 有界纪律（trigger ∈ budget/backlog/drift）。

## Decisions so far

- 跳过计数在 allows()==false 分支独立采集（不合并进 folded 的反面）。

## Not yet specified

- 折入时延 timer（merge 耗时——热点另议）。

## Out of scope

- 沿用 #7–#60。

## Tickets

- [x] [T369 folded/fold-skipped 双 counter 接线](../tickets/T371-fold-counters.md)（impl-246）
- [x] [T370 2 例红队（成功计数/开路跳过不误报）+ 收口](../tickets/T372-fold-counters-close.md)
