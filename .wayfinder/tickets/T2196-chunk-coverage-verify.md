---
id: T2196
title: 索引覆盖计数与失衡定位的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2193
created: 2026-09-14
---

## Question

如何证明覆盖计数与失衡定位？

## Resolution

**用户常设授权 AFK（可推翻）**

`SemanticChunkIndexCoverageTest` 三测全绿（`mvn -pl buzhou-spill -am test`，词包 provider 确定性向量）：空索引零哨兵；双 uri 三切片覆盖精确+最大切片定位（spill://a=2）；provider 不可用短路下覆盖恒空。
