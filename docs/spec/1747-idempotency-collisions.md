# Spec 1747 — 幂等键冲突读面（effort #1747，R48）（effort #1747，R48）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2695–T2696，impl 1348，impl Stripe Idempotency-Key 遥测）。借鉴：幂等键重复命中（真重试 vs 键生成缺陷复用）无账：冲突占比异常升高=键生成有问题。

## Problem Statement

`IdempotencyCollisions`（resilience，实例面线程安全）：record(key, replayed)（replayed=同键二次到达；null/空归 _blank_）+distinct 键集有界 256 FIFO 逐出+census（distinctKeys/totalRecords/replayedRecords/collisionRatio 无样本 −1）。与 IdempotencyAdvisor 互补。纯读面 opt-in。

## Solution

作为幂等治理者，collisionRatio 突升 → 键生成缺陷，不是真重试。

## User Stories

1. 17470
2. 17471
3. 17472

## Implementation Decisions

- 17473

## Testing Decisions

- 17474

## Out of Scope

- 17475

## Further Notes

- 17476
