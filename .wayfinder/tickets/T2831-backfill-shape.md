---
id: T2831
title: 缺口回填计划的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

断流后重放「不重不漏」的缺口单怎么生成？（spec 1815 / effort #1815 / R16）

## Resolution

**Kafka offset 补填/Prometheus backfill 思想纯计划 `GapBackfillPlanner`
（core/recovery）**：plan(from, to, present) 在位乱序重复容忍（排序去重）→
BackfillPlan（缺口升序闭区间清单含首尾、largestGapSpan 瓶颈段长、
missingRatio -1 哨兵、complete() 完整性）；空区间 from=to+1 表达；越界
在位值 fail-fast。纯计划零执行，分批并行归宿主。

