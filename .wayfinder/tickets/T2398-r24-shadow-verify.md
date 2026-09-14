---
id: T2398
title: R24 影子读探针接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2397
created: 2026-09-15
---

## Question

N 会话第 24 轮：如何验收？

## Resolution

ShadowProbeWiringTest 四断言：全采样下一致/分歧各一 + 分歧样本环含 k2；
同 key 采样判定 10 次稳定；零率 sampled=0 且零执行；影子故障 errors=1 不抛。
resilience 全量 384 用例零回归。
