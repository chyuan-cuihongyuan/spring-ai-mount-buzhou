---
id: T1580
title: read_range 回读判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1579
created: 2026-09-15
---

## Question

J 会话第 62 轮：ReadRangeStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ReadRangeStatsTest，SpillService 内存骨架——骨架见既有 ReadRangeTool 测试）：完整回读 → reads=1；坏 JSON → parseRejects=1；skill:// 未接线 → skillRejects=1；混合守恒 calls = 五桶和；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='ReadRangeStatsTest'` 绿 + 既有 ReadRangeTool 回归绿。
