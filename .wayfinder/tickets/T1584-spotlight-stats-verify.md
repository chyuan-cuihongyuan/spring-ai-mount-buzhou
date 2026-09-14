---
id: T1584
title: 读侧 Spotlighting 包裹判定读面的验证裁决
type: task
status: closed
closed-at: 2026-09-15
assignee: zcode-j
blocked-by: T1583
created: 2026-09-15
---

## Question

J 会话第 64 轮：SpotlightStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpotlightStatsTest，HookEnvironment/ToolCallContext 骨架——骨架见既有 guard hook 测试）：普通外部输出 → wrapped=1；已包裹内容再入 → alreadyWrappedSkips=1；拦截告示前缀 → noticeSkips=1；error/null 结果 → errorSkips；守恒 invocations = 四桶和；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='SpotlightStatsTest'` 绿 + 既有 SpotlightHook 回归绿。
