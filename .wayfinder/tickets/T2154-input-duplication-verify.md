---
id: T2154
title: 归一化/复读游程/Top 榜的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2153
created: 2026-09-14
---

## Question

如何证明归一化折叠、游程计数与榜单纪律？

## Resolution

**用户常设授权 AFK（可推翻）**

`UserInputDuplicationAuditTest` 六测全绿（`mvn -pl buzhou-core -am test`）：空哨兵；无复读不上榜；归一化折叠（空白/大小写）；最长游程 3（换题后重置）；Top 榜降序且 ≥2 才入；榜内键截断 64 字符。
