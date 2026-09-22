---
id: T2965
title: 配额空间告警门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

配额耗尽的只读门与显式恢复怎么语义化？（spec 1882 / effort #1882 / R83）

## Resolution`

**etcd NOSPACE 语义持态门 `QuotaAlarmGate`（core/policy）**：
onWrite（超配额拒绝并保持 triggered）+ readsAllowed（读永放行）+
usageRatio（用量比可感）+ acknowledge(minFreeBytes)（释放达标才
复位——防清理不足恢复写的抖动）。配额≥1/阈值≥0 fail-fast。落轮
grep 复核无占坑。
