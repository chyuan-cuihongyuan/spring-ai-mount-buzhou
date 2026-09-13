---
id: T1496
title: 加密消息存储操作计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1495
created: 2026-09-14
---

## Question

J 会话第 23 轮：加密存储操作计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（EncryptingMessageStoreStatsTest，复用既有 cipher()/message() 骨架）：append 2 条 → encrypted=2；load → decrypted=2；底层直插旧明文 → load 计 passthrough；已信封载体再 append → 计 passthrough 不重复加密；deleteSession 直通不计。定向 `mvn -pl buzhou-core test -Dtest='EncryptingMessageStoreStatsTest,EncryptingMessageStoreTest'` 绿。
