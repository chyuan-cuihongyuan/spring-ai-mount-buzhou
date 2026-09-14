---
id: T1592
title: 工具配额消耗读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1591
created: 2026-09-15
---

## Question

J 会话第 68 轮：ToolQuotaStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolQuotaStatsTest，HookEnvironment/DefaultToolCallContext 骨架——见既有配额测试）：配额内调用 → allowed 累加；超限 → quotaBlocks=1；未配置工具 → unmanagedSkips=1；守恒 calls = 三桶和；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='ToolQuotaStatsTest'` 绿 + 既有 ToolQuotaHook 回归绿。
