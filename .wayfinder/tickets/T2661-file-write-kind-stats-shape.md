---
id: T2661
title: 文件写型分类读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

FileWriteKindStats 的形状怎么裁决？（spec 1730 / effort #1730 / R31）（spec 1730 验收/裁决）

## Resolution

WriteKind 三闭集 CREATE/OVERWRITE_UNCHANGED/OVERWRITE_CHANGED+record+census+changedShare=(creates+changed)/total −1 哨兵+resetForTest——restic 变更分类思想，分类归宿主喂入纯读面。
