---
id: T2196
title: 去重率派生与 reset 只清计数的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2195
created: 2026-09-14
---

## Question

如何证明计数守恒、去重率派生与 reset 语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`EventDeduplicationStatsTest` 三测全绿（`mvn -pl buzhou-core -am test`）：放行 2/去重 1+比率 1/3 精确+delegate 只收放行者；新实例 -1 哨兵；**reset 只清计数不清环**（同指纹再入仍被去重——语义保留断言）。
