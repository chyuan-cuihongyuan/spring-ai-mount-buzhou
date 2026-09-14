---
id: T1574
title: compact_now 手动压缩判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1573
created: 2026-09-15
---

## Question

J 会话第 59 轮：CompactNowStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CompactNowStatsTest，骨架见既有 CompactNowTool 测试——InMemory store + 真实 compaction 依赖）：成功压缩 → successes=1；无待压缩 → skippeds=1；无 ToolContext → unboundRejects=1；守恒 calls = 四桶和；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='CompactNowStatsTest'` 绿 + 既有 CompactNowTool 回归绿。
