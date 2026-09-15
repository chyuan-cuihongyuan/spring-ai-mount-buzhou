# Spec 1731 — 思考占比读面（effort #1731，R32）（effort #1731，R32）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2663–T2664，impl 1331，impl OpenAI o1 / DeepSeek-R1 推理预算遥测）。借鉴：推理模型的思考段被 ExtractedThinking 抽出，但思考占输出的比例无账——占比直接驱动成本与延迟，预算治理缺依据。

## Problem Statement

`ThinkingRatioStats`（observability/thinking，实例面线程安全）：record(thinkingChars, totalChars)（total<=0 忽略；thinking 钳制 [0,total]）+report（samples/累计/最近一笔占比，无样本 −1）。纯读面 opt-in。

## Solution

作为成本治理者，累计占比 0.6 → 推理预算吃掉六成输出。

## User Stories

1. 17310
2. 17311
3. 17312

## Implementation Decisions

- 17313

## Testing Decisions

- 17314

## Out of Scope

- 17315

## Further Notes

- 17316
