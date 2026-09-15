---
id: T2655
title: 摘要段落均衡读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SummaryBalanceStats 的形状怎么裁决？（spec 1727 / effort #1727 / R28）（spec 1727 验收/裁决）

## Resolution

静态纯函数 analyze(sectionLengths)→BalanceReport(sections/totalChars/largestIndex/smallestIndex/imbalance=maxShare×k，1=均衡 k=独大)；段数<2 −1；全零=1——文档结构均衡思想。
