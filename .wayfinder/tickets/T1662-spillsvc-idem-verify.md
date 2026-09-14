---
id: T1662
title: SpillService 幂等复用读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1661
created: 2026-09-15
---

## Question

J 会话第 101 轮：SpillServiceStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillServiceStatsTest，SpillModule/DiskSpillStore 骨架——见 SpillOffloadHookTest）：阈值内 → belowThreshold；超阈值新内容 → freshStores；同内容重放 → idempotentReuses；守恒恒等式；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='SpillServiceStatsTest'` 绿 + 既有 SpillService 回归绿。
