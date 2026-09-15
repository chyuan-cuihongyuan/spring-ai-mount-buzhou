---
id: T2689
title: 对冲请求节省读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

HedgeStats 的形状怎么裁决？（spec 1744 / effort #1744 / R45）（spec 1744 验收/裁决）

## Resolution

实例面 recordHedge/recordPrimaryWin/recordHedgeWin/recordLatencySaved（负值忽略）四计数+census(hedgeWinRatio 分母=决胜数，无决胜 −1)——tail-at-scale/Envoy hedging 思想，赢率低=白花钱。
