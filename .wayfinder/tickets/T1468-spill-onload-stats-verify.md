---
id: T1468
title: spill 回读命中率读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1467
created: 2026-09-14
---

## Question

J 会话第 9 轮：回读命中率读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillOnloadStatsTest，复用 OnloadHookTest 的 TempDir+FileSandbox+DefaultToolCallContext 骨架）：成功回读 attempts=loaded=1；失败回读（文件缺失）attempts=failed=1 且结果为 Block；空 path 参数不计尝试；混合 2 成 1 败 → 守恒 attempts == loaded + failed == 3。定向 `mvn -pl buzhou-spill test -Dtest='SpillOnloadStatsTest,OnloadHookTest'` 绿（后者行为回归——计数零改动既有语义）。
