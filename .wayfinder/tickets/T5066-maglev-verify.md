---
id: T5066
title: Q 会话 R33 Maglev 哈希的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5065]
created: 2026-09-18
---

## Question

R33 合同怎么逐一验绿？（spec 3032 / effort #3032 / R33）

## Resolution

**验证通过**：MaglevHashTest 六测全绿——M=13 三节点份额各 [4,5]
和恰 13、同键跨实例确定性、3→4 节点千键迁移率 [0.15,0.40]（期望
~1/4）、重复 3:1 份额比 [2.0,4.5]、千键全落登记节点、五路参数
fail-fast。
