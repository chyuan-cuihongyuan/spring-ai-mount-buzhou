---
id: T1321
title: gate 历史按数据集过滤读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 55 轮：spec 914 的 gate 历史（环形 16 条）——按数据集过滤的查询视图（多数据集共用一个 gate 实例时「只看 ds-X 的判定」）是否有缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 55 轮 = effort #956 / spec 956 / impl 694 续）：缺口成立。落点 `EvalGate.historyOf(String datasetName)`：环形史过滤投影（datasetName 精确匹配，保持新→旧序，null/blank fail-fast）——返回 `List<GateDecision>`。纯查询视图（914 环形史零变化）。
