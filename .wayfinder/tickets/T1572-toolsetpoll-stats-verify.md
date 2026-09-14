---
id: T1572
title: MCP 工具集轮询提供器读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1571
created: 2026-09-15
---

## Question

J 会话第 58 轮：ToolSetPollStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolSetPollStatsTest，InMemoryToolSetSpecStore 骨架——直接调 checkAndFire 私有性则经 replaceAll 触发 listener；用短轮询间隔或反射可见入口）：无变更轮 → unchangedPolls；replaceAll 变更 → changesDetected=1；抛错 store（匿名实现 loadAll 抛 RuntimeException）→ pollFailures=1；守恒 polls = 三桶和；resetForTest 归零。定向 `mvn -pl buzhou-mcp -am test -Dtest='ToolSetPollStatsTest'` 绿 + 既有 DbToolSetProvider 回归绿。
