---
id: T2414
title: R32 退避抖动模式的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2413
created: 2026-09-15
---

## Question

N 会话第 32 轮：如何验收？

## Resolution

JitterModeTest 四断言（反射驱动 computeBackoff 200-300 采样值域）：
EQUAL ∈ [1, 240]（cap200 ±20%）；FULL ∈ [1,200] 且 <40 与 >160 均有落点；
DECORRELATED ∈ [100, 200]（base100 界）；parse fail-fast。
resilience 393 用例零回归。
