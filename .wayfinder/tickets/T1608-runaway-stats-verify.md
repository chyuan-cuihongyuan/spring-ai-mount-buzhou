---
id: T1608
title: Runaway 预算 hook 判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1607
created: 2026-09-15
---

## Question

J 会话第 76 轮：RunawayStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（RunawayStatsTest，StubModelCallContext 骨架同 CounterAtomicitySpreadTest）：放行 → allowed；超步数硬顶 → blocked=1；disabled → disabledSkips=1；守恒恒等式；resetForTest 归零。定向 `mvn -pl buzhou-core -am test -Dtest='RunawayStatsTest'` 绿 + 既有 RunawayHook 回归绿。
