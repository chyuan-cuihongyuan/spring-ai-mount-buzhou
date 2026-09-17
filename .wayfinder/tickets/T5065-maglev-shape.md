---
id: T5065
title: Q 会话 R33 Maglev 哈希的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

键→节点路由怎么查表 O(1) 且最小扰动可控分布？（spec 3032 / effort #3032 / R33）

## Resolution

**MaglevHash（core/policy）**：Google Maglev——步进置换抢占填查找
表（均匀份额差 ≤1 槽；增删节点仅 ~1/n 键迁移），nodeOf 查表 O(1)
常数级；候选重复即权重；表大小素数校验；种子复用
DeterministicHash 确定性。与哈希环/jump 成路由三件。
