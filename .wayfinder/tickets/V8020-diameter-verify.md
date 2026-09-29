---
id: V8020
title: V 会话 V10 TreeDiameter 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8019]
created: 2026-09-29
---

## Question

V10 合同怎么逐一验绿？（spec 8009 / effort #8009 / V10）

## Resolution

**验证通过**：三测全绿——链/星/双叉/单点手锚；200 随机
树 vs 全点对 BFS 圣像 max 全等；path 距离==直径；fail-fast。
