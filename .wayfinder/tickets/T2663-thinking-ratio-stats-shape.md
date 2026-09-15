---
id: T2663
title: 思考占比读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ThinkingRatioStats 的形状怎么裁决？（spec 1731 / effort #1731 / R32）（spec 1731 验收/裁决）

## Resolution

实例面 record(thinkingChars, totalChars)（total<=0 忽略，thinking 钳制 [0,total]）+report(samples/totalThinking/totalChars/cumulativeRatio/lastRatio −1 哨兵)——o1/R1 推理预算遥测，千分位记账防浮点累漂。
