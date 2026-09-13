---
id: T1117
title: 导出去重统计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

去重口径（精确/相似）、节省计量、预览隐私如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 9 轮 = effort #808 / spec 808 / impl 561）：`ExportDedupeStats` 纯函数——精确串值键（相似度显式 out-of-scope）；duplicateChars=Σ(count−1)×len+savingsRatio；Top16 preview 截 32 字符（隐私）；空块计 items 不计重复；字符口径与 738 一致。
