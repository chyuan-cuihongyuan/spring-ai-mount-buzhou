---
id: T2146
title: 碰撞组分组/排序/哨兵的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2145
created: 2026-09-14
---

## Question

如何证明碰撞组显形、排序与哨兵语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`HookOrderAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空清单哨兵；唯一 order 零碰撞；同序分组+组内名字典序（mid/zeta——兜底序即脆性所在）；多组 order 升序；全默认 order 构成单组碰撞（最常见脆性形态）。
