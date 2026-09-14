---
id: T2120
title: Jain 公平指数与哨兵/排序语义的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2119
created: 2026-09-14
---

## Question

如何证明 Jain 公式、哨兵与行动面排序正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`FairnessIndexTest` 五测全绿（`mvn -pl buzhou-core -am test`）：均匀用量 J=1.0（±1e-9）；990/10 倾斜 J≈0.5101 精确断言+dominantShare=0.99+isFair=false；全零/空 -1 哨兵；份额降序+平局典序；long[] 重载位置命名。
