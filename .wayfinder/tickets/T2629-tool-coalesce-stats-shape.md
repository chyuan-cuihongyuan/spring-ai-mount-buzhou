---
id: T2629
title: 工具合并节省读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ToolCoalesceStats 的形状怎么裁决？（spec 1714 / effort #1714 / R15）（spec 1714 验收/裁决）

## Resolution

实例面：recordGroup(members≥2 记账省 members−1)+recordLatencySaved 负值忽略+snapshot→CoalesceSavings(groups/callsJoined/savedCalls/latencySaved/savingRatio 无合并 −1)+resetForTest——Go singleflight 合并回喂遥测。
