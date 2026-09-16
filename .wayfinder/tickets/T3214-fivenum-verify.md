---
id: T3214
title: 五数概括与 IQR 围栏的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3213]
created: 2026-09-17
---

## Question

FiveNumberSummary 合同（五点/插值/围栏/离群/退化/畸形）怎么钉住？（spec 2056 / effort #2056 / R57）

## Resolution

**八用例一次全绿**（buzhou-core；实现首版构造残留参数已修）：1..9
恰 1/3/5/7/9 IQR=4 / 偶数 R-7 插值 2.5/1.75/3.25 手算 / 围栏 [−3,13]
/ 100 极端值离群、13 箱内、恰上界不离 / 单点退化围栏=箱体 / 乱序
同结果 / 系数 3.0 放宽围栏 / 畸形四型（null、空、NaN、负系数）
fail-fast。
