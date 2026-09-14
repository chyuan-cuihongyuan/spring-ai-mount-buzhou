---
id: T1594
title: 危险工具守卫判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1593
created: 2026-09-15
---

## Question

J 会话第 69 轮：DangerousToolStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DangerousToolStatsTest，GuardModule yml 装配骨架驱动真实 matcher）：未匹配工具 → unmatchedSkips=1；危险工具 → escalations=1；守恒 invocations = 五桶和；resetForTest 归零。定向 `mvn -pl buzhou-guard -am test -Dtest='DangerousToolStatsTest'` 绿 + 既有 DangerousToolGuard 回归绿。
