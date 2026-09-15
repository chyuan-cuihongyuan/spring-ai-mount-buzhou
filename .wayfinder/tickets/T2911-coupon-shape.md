---
id: T2911
title: 收藏家覆盖期望的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

覆盖测试「跑多少轮才能见全」怎么预算？（spec 1855 / effort #1855 / R56）

## Resolution`

**概率论 coupon collector 思想纯投影 `CouponCollectorProjection`
（core/eval）**：expectedDraws(k)=k×H(k)（调和级数长尾——收齐 10 类期望
~29 轮非 10 轮）+ expectedRemaining(seen,k)=k×(H(k)−H(s)) 随进度递减；
double 全程防溢出；0≤seen≤k 契约。覆盖轮数预算从拍到有期望依据。

