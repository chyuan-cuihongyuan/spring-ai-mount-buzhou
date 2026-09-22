---
id: T2993
title: 事务号余量分级的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

单调编号耗尽前的分级预警怎么判定？（spec 1896 / effort #1896 / R97）

## Resolution`

**Postgres xid wraparound 语义纯计算 `XidHeadroomGuard`
（core/recovery）**：urgency 四级（OK/WARN/CRITICAL/EXHAUSTED，
边界含上）+ headroom 余量读数（负=超发诚实显示）。limit≥1/分级线
单调且越界 fail-fast。落轮 grep 复核 HPA 稳定窗已被 BulkheadScaling
Advisor（923）占坑换静脉。
