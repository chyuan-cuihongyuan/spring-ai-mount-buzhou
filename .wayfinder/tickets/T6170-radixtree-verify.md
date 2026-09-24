---
id: T6170
title: S 会话 S35 Radix Tree 基数树最长前缀路由的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6169]
created: 2026-09-24
---

## Question

S35 合同怎么逐一验绿？（spec 5034 / effort #5034 / S35）

## Resolution

**验证通过**：RadixTreeTest 五测全绿——最长前缀胜出；部分
命中分裂（roman 经典族）；终态与插入序无关（nodeCount=4
正逆序全等）；压缩节点数钉住（4 节点 2 键）；畸形 fail-fast。
