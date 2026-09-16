---
id: T3229
title: Z 序曲线的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多维键进一维索引怎么保局部性？（spec 2064 / effort #2064 / R65）

## Resolution

**Morton 码纯函数 `ZOrderCurve`（core/recovery）**：encode 位交织
（x 偶位 y 奇位，int×32→long 恰容）——格内紧致（x,y<2ⁿ → 标量<2²ⁿ，
多维子空间落有界标量段，范围查询变区间）+decode 互逆含极值往返
+诚实边界（逐对紧界不成立——借位致 Z 形跳变，语义是格内/渐近）。
