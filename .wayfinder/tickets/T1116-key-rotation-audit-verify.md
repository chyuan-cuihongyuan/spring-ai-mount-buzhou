---
id: T1116
title: 密钥轮换到期审计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1115]
created: 2026-09-13
---

## Question

三档边界/排序/降级面如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 8 轮 = effort #807）：KeyRotationAuditTest 6 例——边界四点（恰 maxAge=OVERDUE、999 临期窗内 DUE_SOON、恰临期线 DUE_SOON、500 OK）/最坏排序 6→7→8→5 含同级 age 降序/UNKNOWN_ACTIVE 置顶/无钥如实/脏账跳过/fail-fast×4。首跑边界预期笔误修正（实现与 spec 一致）。
