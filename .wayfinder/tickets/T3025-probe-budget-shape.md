---
id: T3025
title: 探测流量预算的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

探测流量的占比预算怎么判定？（spec 1912 / effort #1912 / R113）

## Resolution`

**SRE 健康检查预算惯例纯计算 `ProbeBudget`（core/health）**：
probeShare（探测 QPS/容量 QPS 占比读数）+ verdict 两态（≤ maxShare
WITHIN/> OVER——探活变压死事前可见）。QPS 非负/capacity≥1/maxShare
∈(0,1] fail-fast。落轮 grep 复核无占坑。
