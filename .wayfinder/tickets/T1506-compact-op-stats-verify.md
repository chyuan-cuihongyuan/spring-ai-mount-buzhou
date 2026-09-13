---
id: T1506
title: 手动压缩操作分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1505
created: 2026-09-14
---

## Question

J 会话第 28 轮：操作分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CompactOpStatsTest，复用 ManualCompactorTest 的 StubSummaryModel/内存仓骨架）：完成路径 completed=1 且 foldedMessages 累计；幂等再压缩 skipped=1；失败桩（模型抛错）failed=1；守恒 completed + skipped + failed == attempts；fresh 零值行。定向 `mvn -pl buzhou-memory test -Dtest='CompactOpStatsTest,ManualCompactorTest'` 绿（后者回归幂等/统计既有语义）。
