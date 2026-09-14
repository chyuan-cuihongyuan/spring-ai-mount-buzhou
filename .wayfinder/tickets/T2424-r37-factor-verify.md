---
id: T2424
title: R37 校准系数建议的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2423
created: 2026-09-15
---

## Question

N 会话第 37 轮：如何验收？

## Resolution

CalibrationFactorSuggestionTest 三断言：恒高估 25%×20 对 → 建议 ∈ (0.7,1)；
恒低估 → >1；样本 5<10 与零偏差 20 对 → 双 empty。校准域 10 用例零回归。
