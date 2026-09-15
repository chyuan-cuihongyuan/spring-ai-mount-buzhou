---
id: T2662
title: 文件写型分类读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2661
created: 2026-09-15
---

## Question

FileWriteKindStats 怎么验证？（spec 1730 验收/裁决）

## Resolution

FileWriteKindStatsTest：空哨兵/四笔 1:2:1+share=0.5/reset。
