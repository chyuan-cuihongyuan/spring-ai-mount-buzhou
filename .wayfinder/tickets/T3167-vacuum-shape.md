---
id: T3167
title: 维护触发器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

死数据清理的触发时机怎么双口径定义？（spec 2033 / effort #2033 / R34）

## Resolution

**Postgres autovacuum 纯判定触发器 `MaintenanceTrigger`
（core/recovery）**：shouldTrigger 双口径——死/活 ≥ 阈值（默认 0.2；
live=0 全死必清、双零不触）**或**距上次触发 ≥ 最大间隔（默认 24h，
从未触发自 0 起算——低流量兜底防陈化）+noteTriggered 记账（次数/
时刻，间隔重算——触发频率即维护健康度）。
