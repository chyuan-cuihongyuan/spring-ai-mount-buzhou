---
id: T2963
title: 纠删码冗余预算的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

EC(k,m) 的可用率/容忍度/修复读怎么统一计算？（spec 1881 / effort #1881 / R82）

## Resolution`

**MinIO/Ceph EC 语义纯计算 `ErasureCodingBudget`（core/policy）**：
usableRatio（k/(k+m)）+ tolerableFailures（=m 可丢片数）+
repairReads（=k 修复读放大）+ totalShards（k+m 部署下限）。data≥1/
parity≥1 fail-fast（零冗余非法）。落轮 grep 复核：纠删码/shard 无
占坑。
