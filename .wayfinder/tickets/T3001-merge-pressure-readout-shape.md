---
id: T3001
title: 合并压力读面的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

合并积压的压力刻度与插入安全阀怎么定？（spec 1900 / effort #1900 / R101）

## Resolution`

**ClickHouse too-many-parts 语义纯计算 `MergePressureReadout`
（core/recovery）**：pressure（活跃段/建议上限占比）+ verdict 三态
（OK/WARN/REJECT 边界含上）+ shouldRejectInsert（硬上限独立安全阀）。
active≥0/上限≥1/warnAt∈(0,1] fail-fast。落轮 grep 复核无占坑。
