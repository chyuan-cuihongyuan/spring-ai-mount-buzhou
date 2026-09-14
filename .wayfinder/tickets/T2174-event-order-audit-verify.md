---
id: T2174
title: 逆序对计数与定位哨兵的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2173
created: 2026-09-14
---

## Question

如何证明逆序计数、倒退量与定位哨兵？

## Resolution

**用户常设授权 AFK（可推翻）**

`EventOrderAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空输入哨兵；单调序列零逆序；等时刻不算逆序；**时钟倒退显形**（倒退 8 秒→inversions=1/maxInversion=8000/firstIndex=2 定位）；null 时戳跳过不崩。
