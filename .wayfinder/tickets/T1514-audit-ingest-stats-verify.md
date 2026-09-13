---
id: T1514
title: 审计收集器采集与持久化失败计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1513
created: 2026-09-14
---

## Question

J 会话第 32 轮：采集与持久化失败计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（AuditIngestStatsTest，复用既有 AuditChain/InMemoryAuditRecordStore/抛错 store 骨架）：审计类型事件 collected=1；非审计类型不计；持久化失败 persistFailures=1 且链照常入链可验；收尾广播后 openSessions 清零；fresh 零值。定向 `mvn -pl buzhou-guard test -Dtest='AuditIngestStatsTest,AuditTrailCollectorTest'` 绿。
