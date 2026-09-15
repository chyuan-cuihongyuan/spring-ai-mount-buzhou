---
id: T2923
title: 写偏斜检测的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

快照隔离下的组合不变量破坏怎么显形？（spec 1861 / effort #1861 / R62）

## Resolution`

**数据库快照隔离 write skew 异象（经典「值班医生」反例）纯检测
`WriteSkewDetector`（core/transaction）**：Transaction(id, readSet,
writeSet) 契约 + skewRisk(a,b)（双方读写非空 且 读交 且 写不交——写冲突
检测拦不住的组合风险）+ scan 全对扫描（i<j 确定性）→ 风险对清单（直接
映射冲突桌）。纯检测不拦截。

