---
id: T1143
title: 目录 lint 严重度分级的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

严重度分级做进 linter 还是独立纯函数？默认映射与扩展语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 22 轮 = effort #821 / spec 821 / impl 574）：`LintSeverityGrader` 独立纯函数（linter 零变更）——DENY/WARN/HINT 三档+默认映射+withRule 不可变定制+未知归 HINT+严重序典序破平。
