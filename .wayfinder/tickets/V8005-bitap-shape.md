---
id: V8005
title: V 会话 V3 BitapSearch 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

短模式搜索怎么按位并行？（spec 8002 / effort #8002 / V3）

## Resolution

**BitapSearch（core/metrics）**：Shift-And 单字位并行——
字符掩码表+状态左移进位与高位命中判定；模式长 ≤63 位宽
诚实拒绝；findAll 全部（含重叠）命中。
