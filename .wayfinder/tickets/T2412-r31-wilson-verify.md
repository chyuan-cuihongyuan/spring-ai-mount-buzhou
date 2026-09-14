---
id: T2412
title: R31 Wilson 置信区间的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2411
created: 2026-09-15
---

## Question

N 会话第 31 轮：如何验收？

## Resolution

WilsonIntervalTest 四断言：7/10 区间 (0,0.7)∪(0.7,1]；全胜/全败不越界；
7/10 区间宽于 700/1000；退化输入零区间。PairwiseEvalRunnerTest 11 用例零回归。
