# 1446 — 轮次采样漏斗读面

> 来源：L 会话第 49 轮 = effort #1446（票 T2199 / T2200 / impl 1101）。借鉴：Envoy access log sampling（采样决策漏斗——「为何这条没进数据集」按原因显形而非静默消失）。

## Problem Statement

`TurnSamplerHook`（轮次→评估数据集采样：哈希确定性判定）无漏斗读面：ratePercent 过滤、短问过滤、空输入跳过、写入失败各环节的量无数据——「采样率 20% 为何数据集不涨」无法归因（rate 外？短问？写入失败？）。

## 目标

- `TurnSamplerHook`（core/eval）静态漏斗增量：
  - `turnsSeen`（非空问+答进入判定的轮次）/ `emptySkipped`（缺问/缺答）/ `shortSkipped`（<minInputChars 短问）/ `rateSkipped`（哈希采样率外）/ `written`（写入成功）/ `writeFailures`（fail-soft 写入失败）；
  - 桶口径：turnsSeen = written + rateSkipped + shortSkipped + emptySkipped（ratePercent=0 时早退不入漏斗——零开销既有语义）；
  - `samplerStats()` 静态快照 + `resetSamplerStatsForTest()`。
- 采样判定（哈希确定性）/fail-soft/CONTINUE 语义逐位不变。

## 兼容性

纯增量读面：ratePercent=0 早退不入漏斗（与关闭语义一致）；埋点只增记账。

## Out of Scope

- 写入失败重试（fail-soft 既有语义）。
- 按会话分桶（基数红线）。
- 数据集侧容量治理（EvalDatasetStore 域）。
