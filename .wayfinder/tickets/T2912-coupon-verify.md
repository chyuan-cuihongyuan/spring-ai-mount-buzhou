---
id: T2912
title: 收藏家覆盖期望的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2911]
created: 2026-09-16
---

## Question]

期望投影在已知值/长尾/单调/畸形四面下正确吗？（spec 1855 / effort #1855 / R56）

## Resolution`

**CouponCollectorProjectionTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=CouponCollectorProjectionTest）：k=1/2/3 → 1/3/5.5 精确；k=10 ∈
(29,30) 长尾；余轮 seen=10 归零、seen=0=全量、5/10 居间单调；负类数与
进度越界 fail-fast。

