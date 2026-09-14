---
id: T1582
title: 双时序事实台账操作读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1581
created: 2026-09-15
---

## Question

J 会话第 63 轮：FactLedgerStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FactLedgerStatsTest，InMemory SessionStateStore 骨架——骨架见既有 BiTemporalFactLedger 测试）：recordSuperseded → supersededWrites=1；historyOf/validAt 各一次 → 两查询桶；手工注入坏 JSON 键后 load → corruptRecordLoads=1；resetForTest 归零。定向 `mvn -pl buzhou-memory -am test -Dtest='FactLedgerStatsTest'` 绿 + 既有 BiTemporalFactLedger 回归绿。
