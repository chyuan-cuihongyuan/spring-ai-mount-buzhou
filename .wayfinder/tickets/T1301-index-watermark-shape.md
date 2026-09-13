---
id: T1301
title: 会话索引存量水位读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 26 轮：InMemorySessionIndexStore（索引内存实现）是否有水位读面缺口？同构扩散自 spec 924（ObservabilityStore 水位）。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 26 轮 = effort #925 / spec 925 / impl 678）：同构扩散成立——索引是会话治理第一查询面（分页/游标），存量水位（索引条目数 vs 上限）贴顶即「新会话不可发现」的前兆。落点 `InMemorySessionIndexStore`（internal）新增 `watermark()`：`record Watermark(int indexedSessions, int maxSessions)`（索引条目数与上限直通；上限为 0/无界时 maxSessions=-1 显式标注无界——诚实口径）。纯读面零行为变化。若实现无上限概念则 maxSessions=-1 语义入档（内存实现受全局 session 上限间接约束时照实标注）。
