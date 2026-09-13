---
id: T1197
title: 配置默认偏离审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

vs 出厂默认的偏离对账与 ConfigDiff 如何辨义？无基线键语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 49 轮 = effort #848 / spec 848 / impl 600 续）：`ConfigDeviationAudit` 纯函数——String.equals 比较+无基线不裁决+偏离清单典序封顶 32+偏离率；与 ConfigDiff（快照间）辨义（vs 出厂默认）。
