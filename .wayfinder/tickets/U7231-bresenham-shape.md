---
id: U7231
title: U 会话 U16 BresenhamLine 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

光栅走格怎么免浮点漂移？（spec 7015 / effort #7015 / U16）

## Resolution

**BresenhamLine（core/policy）**：误差项符号判定整数
走格；含端点；格数=max(|dx|,|dy|)+1；同参数同序列。
