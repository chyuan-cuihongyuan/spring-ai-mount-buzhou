---
id: T1692
title: RangeReadEngine 引擎读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1691
created: 2026-09-15
---

## Question

J 会话第 116 轮：EngineReadStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EngineReadStatsTest，纯字符串骨架）：bytes/window/json 各模式一调用 → 三桶各 1；守恒 engineCalls = 三桶和；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='EngineReadStatsTest'` 绿 + 既有 RangeReadEngine 回归绿。
