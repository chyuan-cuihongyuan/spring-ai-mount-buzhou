# Spec 93 — A/B run 指纹（effort #54）

> wayfinder map：`.wayfinder54/MAP.md`（T351–T352）。spec 82 fog 项收口
> （「ab.run 记录同字段是零成本扩展」）。

## Problem Statement

A/B run 记录（spec 74/76）无数据集版本标识：数据集就地改条目后，新旧 A/B 结论
不可比且不可验——spec 82 指纹在 eval run 已落，A/B 面缺席。

## Solution

`PairwiseEvalRunner.compare()` 计算执行时刻数据集指纹
（`EvalDatasetStore.fingerprint`，内容寻址）：
- `PairwiseEvalResult` 增 `datasetFingerprint`（7 参 record；6 参旧构造兼容）；
- 落盘 map 携带（null 不写键——旧记录/未指纹场景解码自然 null）；
- `abRuns` 摘要行与 `abRun` 明细回读均携带。

## User Stories

1. 作为评估作者，我要 A/B 结论带版本戳，所以数据集演进后结论适用性可验。
2. 作为红队，我要落盘-回读-摘要三面等值，所以指纹无漂移。

## Implementation Decisions

- null 不写键（诚实：无字段 = 旧记录；不写哨兵空串）。
- 零新类型零新键（字段级扩展——二进制兼容经旧构造保留）。

## Testing Decisions

- 内存面 = fingerprint() 等值；abRun 明细回读等值；abRuns 摘要行等值；
- 既有 PairwiseEvalRunnerTest 全族回归（事件/落盘/明细/幂等不漂移）。

## Out of Scope

- AB run JSONL 导出；A/B 对比 diff 面。

## Further Notes

- 与 spec 82/81 组合：数据集演进全链可验（eval run 与 ab run 同一版本锚）。
