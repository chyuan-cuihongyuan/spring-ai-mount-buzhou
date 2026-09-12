---
id: T895
title: 归档互斥验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T894
created: 2026-09-12
---

## Question

互斥语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionArchiverMutexTest 3/3 + core 全模块零回归；门/探针经 withContributor 钩子注入——SessionCleaner final 不可继承）：

- 同会话并发双归档：恰一 true（第二个见空会话 false）。
- archive 在 live-delete 中被钩子阻塞时，同会话 restore 被互斥挡住（isDone=false）；archive 完成后 restore 成功且消息还原在（丢失窗关闭）。
- 跨会话两归档均完成无死锁。
