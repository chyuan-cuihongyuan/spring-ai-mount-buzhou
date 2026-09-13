---
id: T1118
title: 导出去重统计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1117]
created: 2026-09-13
---

## Question

计数/节省/排序/口径边界如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 9 轮 = effort #808）：ExportDedupeStatsTest 6 例——hello×3 浪费 10 字符+ratio 1e-9/全唯一零节省/空块四种形态计 items 不计重复/Top 降序 80>12>3/预览 33 字符含省略号/空列表+fail-fast。
