---
id: T1004
title: 归档 PDB yml 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1003
created: 2026-09-13
---

## Question

capped probe 三态判定与 spec 704 全量语义一致？min=0 保护最后会话？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 27 轮）：① 10 会话 min=2 → 放行；2 会话 min=2 → 拒；0 会话 min=2 → 拒；② min=0 语义：0 会话拒（保护最后一个）、1 会话放行；③ spec 704 既有用例零回归；④ 全模块回归绿。
