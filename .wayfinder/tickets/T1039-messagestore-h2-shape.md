---
id: T1039
title: MessageStore 契约接入 H2 的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

MessageStoreContract（spec 743）需真实 SQL 存储接入示范。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 45 轮 = effort #745 / spec 744 / impl 547，测试域轮）：H2StoresContractTest 增 messageStoreSatisfiesGSessionContract——spec 732 同模式（H2 无 Docker CI 口径）。
