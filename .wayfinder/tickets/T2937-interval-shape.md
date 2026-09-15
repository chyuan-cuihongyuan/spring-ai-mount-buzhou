---
id: T2937
title: 区间调度的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

占用段合并与空闲档期怎么基建？（spec 1868 / effort #1868 / R69）

## Resolution`

**日历调度惯例（busy/free）纯计算 `IntervalSchedule`（core/exec）**：
Interval（start≤end 契约，零长合法）+ merge（排序扫描——重叠/相邻/嵌套
归一取 max end，乱序容忍）+ gaps（窗口内首前/区间间/尾后缝，越界裁剪，
早退优化）。排程前置基建，每处不再手搓。

