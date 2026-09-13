---
id: T1115
title: 密钥轮换到期审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

密钥龄审计放环内还是独立纯函数？临期/超档判据与异常面如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 8 轮 = effort #807 / spec 807 / impl 560）：`KeyRotationAudit` 独立纯函数（环零侵入）——OVERDUE/DUE_SOON/OK 三档+UNKNOWN_ACTIVE 异常面；最坏排序；激活账由 persister 侧提供；warnBefore>maxAge 等 fail-fast。
