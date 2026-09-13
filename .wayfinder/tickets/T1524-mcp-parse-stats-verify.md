---
id: T1524
title: MCP properties 装配解析统计读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1523
created: 2026-09-14
---

## Question

J 会话第 36 轮：装配解析统计读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（McpParseStatsTest，Map 直构 fromServersMap 骨架）：双 server 各 2 binding → servers=2/bindings=4/skipped=0；非 Map server 值 → IllegalArgumentException（既有 fail-fast 回归）；bindings 清单含非 Map 项 → skipped 计数且其余照常；resetForTest 归零。定向 `mvn -pl buzhou-mcp test -Dtest='McpParseStatsTest'` 绿 + 既有装配回归绿。
