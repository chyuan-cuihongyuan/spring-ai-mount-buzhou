---
id: T2645
title: 回放时钟偏斜读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ReplaySkewStats 的形状怎么裁决？（spec 1722 / effort #1722 / R23）（spec 1722 验收/裁决）

## Resolution

实例面 record(originalAt, replayedAt)；负偏斜（倒挂）单独计数不混入正偏斜统计（诚实分离）；report(samples/medianLag/maxLag/negativeSkewCount，无正样本 −1)——Kafka lag/NTP 偏斜思想。
