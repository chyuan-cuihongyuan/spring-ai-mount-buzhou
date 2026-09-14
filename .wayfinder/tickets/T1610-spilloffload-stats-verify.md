---
id: T1610
title: Spill 溢出 hook 判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1609
created: 2026-09-15
---

## Question

J 会话第 77 轮：SpillOffloadStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillOffloadStatsTest，SpillModule/HookEnvironment 骨架——见既有 spill hook 测试）：超阈值 → offloaded=1；阈值内 → cleanInline；error 结果 → errorSkips；守恒恒等式；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='SpillOffloadStatsTest'` 绿 + 既有 SpillOffloadHook 回归绿。
