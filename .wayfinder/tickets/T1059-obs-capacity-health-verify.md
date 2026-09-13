---
id: T1059
title: 内存观测库容量健康面验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1058]
created: 2026-09-13
---

## Question

容量与逐出读数如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 30 轮 = effort #729）：容量 2 写 3 会话→used=2/evicted=1/utilization=1.0；null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
