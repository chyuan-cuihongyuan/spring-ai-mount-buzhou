---
id: T1161
title: 审计树形健康读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

树形健康（深度/满树/补位）读数的口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 31 轮 = effort #830 / spec 830 / impl 583）：`AuditTreeHealthReadout` 纯函数——深度=32−lz(n−1)、nextPow2、补位、满树（n=2^k 含 1）；0 空树非满；负归 0；叶数调用方采集。
