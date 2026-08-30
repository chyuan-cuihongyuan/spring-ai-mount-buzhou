# Wayfinder Map — Buzhou A/B run 明细查询（effort #37，50 轮自迭代第 2 轮）

> effort #37，延续 #36（T303–T304 / impl-222）。主线：**#36 fog 毕业生**——
> `abRuns` 只有摘要行，per-item 裁决明细在 JSON 里但没有查询面；eval 侧有
> `EvalQueryService.run(runId)`，ab 侧缺对称入口。

## Destination

`PairwiseEvalRunner.abRun(store, runId)` 静态明细回读（`Optional<PairwiseEvalResult>`，
verdict 面：winner/reason/error；输出原文不落盘故回读 null——spec 74 决策沿用）；
`mapToResult` 解码底座与 `abRuns` 共用；未知 runId = empty；ab./eval. 前缀互不误命中。

## Notes

- 借鉴：LangSmith run detail API（list + get 两级查询面）。
- 查询不触写路径（与 EvalQueryService 同纪律）。

## Decisions so far

- 明细回读不含输出原文（落盘形态决定——不为此扩 schema）。

## Not yet specified

- run 注册表 gauge（#38 候选）；事务性并行批；会话归档冷层；语义漂移触发压缩；
  outbox due-time 键序；RediSearch 向量缓存；优先级调度。

## Out of scope

- 沿用 #7–#36；明细分页（数据集规模未到 pain point）。

## Tickets

- [x] [T305 abRun 明细回读 + mapToResult 解码底座](tickets/T305-ab-run-detail.md)（impl-223）
- [x] [T306 红队（verdict 等值/未知 empty/前缀隔离）+ 文档 + 收口](tickets/T306-ab-detail-close.md)
