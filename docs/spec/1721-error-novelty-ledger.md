# Spec 1721 — 错误首见签名台账（effort #1721，R22）（effort #1721，R22）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2643–T2644，impl 1321，impl Sentry new-issue 追踪）。借鉴：错误签名反复出现是已知病，新签名出现才是新病——ErrorSignatures 管规范化不管新颖性，回归探测的第一信号缺位。

## Problem Statement

`ErrorNoveltyLedger`（core/metrics，实例面 synchronized）：record(signature) 返回是否首见；签名集合有界默认 256 FIFO 逐出最旧（长跑下老签名被逐出后再现会再次「首见」——诚实入档）；report→NoveltyReport(seenDistinct/newCount/repeatCount/noveltyRatio 无样本 −1)；null/空签名归 _blank_ 桶。

## Solution

作为值守者，record 返回 true → 告警（新病）。

## User Stories

1. 17210
2. 17211
3. 17212

## Implementation Decisions

- 17213

## Testing Decisions

- 17214

## Out of Scope

- 17215

## Further Notes

- 17216
