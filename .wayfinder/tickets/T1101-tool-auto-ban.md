---
id: T1101
title: 工具自动封禁的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

工具连续失败如何自动止损？滑窗语义、封禁粒度与 watch 集取舍如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 1 轮 = effort #800 / spec 800 / impl 553）：`ToolAutoBanHook`（guard.hook，order 255）——afterTool 观测失败按 (session,tool) 滑窗累计，达 maxViolations 封禁 banSeconds；beforeTool 拦截报剩余秒。fail2ban 语义忠实：成功不重置/窗口自然滑出/到期惰性解除。键封顶 256+truncated、snapshot 读数、Clock 注入、空 watch=零行为。
