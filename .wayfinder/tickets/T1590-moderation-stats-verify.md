---
id: T1590
title: 内容安全词表双缝判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1589
created: 2026-09-15
---

## Question

J 会话第 67 轮：ModerationStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ModerationStatsTest，HookEnvironment/ToolCallContext 骨架——同族 SpotlightStatsTest）：BLOCK 命中 → blocked；MASK 命中 → masked；无命中 → cleanSkips；null 输入 → nullSkips；守恒 invocations = 四桶和；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='ModerationStatsTest'` 绿 + 既有 ContentModerationHook 回归绿。
