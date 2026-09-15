---
id: T2643
title: 错误首见签名台账的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ErrorNoveltyLedger 的形状怎么裁决？（spec 1721 / effort #1721 / R22）（spec 1721 验收/裁决）

## Resolution

record(signature) 返回是否首见；集合有界默认 256 FIFO 逐出（逐出后再现再「首见」诚实入档）；report(seenDistinct/newCount/repeatCount/noveltyRatio −1 哨兵)；null/空归 _blank_——Sentry new-issue 思想。
