---
id: T1040
title: MessageStore 契约接入 H2 的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1039
created: 2026-09-13
---

## Question

JdbcMessageStore 过四项契约（时序保序/幂等清场）？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 45 轮）：H2 契约方法全绿 + store-jdbc 模块回归。
