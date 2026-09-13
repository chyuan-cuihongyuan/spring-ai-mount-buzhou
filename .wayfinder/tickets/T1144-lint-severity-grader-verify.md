---
id: T1144
title: 目录 lint 严重度分级验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1143]
created: 2026-09-13
---

## Question

默认映射/排序/不可变定制如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 22 轮 = effort #821）：LintSeverityGraderTest 5 例——三档计数+严重序/定制覆盖+未知 HINT/withRule 脏入参同实例+不可变隔离/典序破平+null 跳过/空真。
