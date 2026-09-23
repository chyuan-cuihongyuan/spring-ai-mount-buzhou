---
id: T6137
title: S 会话 S19 Read Repair 读修复的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

多副本读怎么顺手修复陈旧副本？（spec 5017a / effort #5018 /
S19）

## Resolution

**ReadRepair（core/transaction）**：Dynamo read repair 思想
——stitch 取最高版本为胜者（并列副本名字典序 tie-break），
低于胜者者入陈旧清单（字典序确定性）供回写；空汇报/负版本
fail-fast。
