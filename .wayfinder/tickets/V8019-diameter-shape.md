---
id: V8019
title: V 会话 V10 TreeDiameter 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

树最长链怎么一次遍历？（spec 8009 / effort #8009 / V10）

## Resolution

**TreeDiameter（core/concurrent）**：后序子树 DP——每节点
top1/top2 深度拼链取 max；diameter/path 双面（端点并列
取小编号 canonical）；单亲树形校验 fail-fast。
