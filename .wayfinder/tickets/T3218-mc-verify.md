---
id: T3218
title: 多重比较校正的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3217]
created: 2026-09-17
---

## Question

MultipleComparisonCorrection 合同（保守界/逐步停步/不弱于/退化/畸形）怎么钉住？（spec 2058 / effort #2058 / R59）

## Resolution

**七用例一次全绿**（buzhou-core）：Bonferroni m=5 恰 p≤0.01 两显 /
Holm 阈值序列 [0.0125,0.0167,0.025,0.05] 逐步两显 / 乱序入参停步后
索引序断言 / Holm⊇Bonferroni 逐索引含 6 指标 / 全大 p 双法零显 /
m=1 双法退化 p≤α / 畸形六型（null、空、α 0、p 1.5、−0.1、NaN）
fail-fast。
