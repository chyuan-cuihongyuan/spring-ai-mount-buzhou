---
id: T2184
title: 冷热三桶与占比派生的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2183
created: 2026-09-14
---

## Question

如何证明三桶分桶、占比派生与钳 0 口径？

## Resolution

**用户常设授权 AFK（可推翻）**

`SpillTieringAuditTest` 四测全绿（`mvn -pl buzhou-spill -am test`）：空库 -1 双哨兵；5 存量（2 never/1 single/1 multi 热点 4 次读）三桶精确+双占比；**读事件超存量钳 never=0**（历史读含已删 uri 口径）；阈值边界 2 次即热点。
