# Spec 1741 — 注入分类校准探针（effort #1741，R42）（effort #1741，R42）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2683–T2684，impl 1341，impl HuggingFace evaluate / 混淆矩阵）。借鉴：InjectionClassifier 判定与真实注入无对照账：误杀真请求与放走注入代价不对称，precision/recall 不可见。

## Problem Statement

`InjectionCalibrationProbe`（guard，实例面线程安全）：record(predictedMalicious, actuallyMalicious) 四象限（TP/TN/FP/FN）归账+report（precision/recall/accuracy 分母 0 哨兵 −1）+resetForTest。PiiProbeSelfCheck 同族先例。纯读面 opt-in。

## Solution

作为阈值调参者，precision 低 → 误杀多，阈值放松。

## User Stories

1. 17410
2. 17411
3. 17412

## Implementation Decisions

- 17413

## Testing Decisions

- 17414

## Out of Scope

- 17415

## Further Notes

- 17416
