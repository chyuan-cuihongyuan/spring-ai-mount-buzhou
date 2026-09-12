---
id: T1038
title: MessageStore SPI 契约校验套件的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1037
created: 2026-09-13
---

## Question

InMemory 全绿？deleteSession no-op 走样红？探针零残留？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 44 轮）：① InMemory 4/4 全绿；② deleteSession no-op 走样实现 delete-session-idempotent 红；③ 探针会话全覆盖清理；④ 报告不可变。`mvn -pl buzhou-core -am test` 全绿。
