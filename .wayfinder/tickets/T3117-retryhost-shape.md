---
id: T3117
title: 重试主机排除的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

重试落同一坏端点怎么避免？（spec 2008 / effort #2008 / R9）

## Resolution

**Envoy retry host predicate 线程安全让位表 `RetryHostExclusion`
（buzhou-resilience routing）**：失败记冷却起点（再失败顺延）+
filterCandidates 剔冷却窗内失败者（候选序保持——路由权重序不动）+
全排除回退全量（排除是偏好不是硬门，不空转）+ excludedCount 全排除
回退态显形 + 时间外注入确定性。默认冷却 30s。
