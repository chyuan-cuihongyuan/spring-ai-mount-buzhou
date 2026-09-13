---
id: T1309
title: 租约契约接入 H2/JDBC store 的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 30 轮：spec 922 的 SessionLeaseStoreContract 只有内存实现接入——JdbcSessionLeaseStore（真实 SQL 语义）过九项契约是否有验证缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 30 轮 = effort #927 续 / spec 928 补 / impl 682）：接入成立。落点 store-jdbc 测试域 `H2LeaseContractTest`：H2 内存库（`JdbcBuzhouStores.createWithRecovery` 既有测试基建）构造 `JdbcSessionLeaseStore` → `SessionLeaseStoreContract.verify` 九项全过。真实 SQL 语义（fence 单调 via DB 序列/UPDATE where、renew 原子 compare-and-set）经契约逐项实证——第三方 JDBC store 的「多实例安全」自证价值主张落地。若契约抓出 JdbcSessionLeaseStore 语义缺陷按 G r39 先例修复。
