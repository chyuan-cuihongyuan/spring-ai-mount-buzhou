---
id: T2308
title: load 快路径的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2307
created: 2026-09-15
---

## Question

M 会话第 32 轮：快路径如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：InMemoryStoresContractTest 14 用例（排序语义契约）+ InMemoryMessageStoreTest 4 用例（新增乱序插入 load 正确排序——快路径回退验证）全绿。
