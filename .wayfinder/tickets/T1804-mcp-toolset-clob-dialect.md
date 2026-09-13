---
id: T1804
title: JdbcToolSetSpecStore DDL 方言缺陷——CLOB 在 PostgreSQL 不存在
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-14
---

## Question

T1801 R1 补测期间审计发现：JdbcToolSetSpecStore 自含 DDL 用 `spec_json CLOB`——真实 PostgreSQL 是否支持？修复形态？

## Resolution

**用户常设授权 AFK（可推翻）**

实证（postgres:17 容器直测，2026-09-14）：`CREATE TABLE ... spec_json CLOB ...` → `ERROR: type "clob" does not exist`；`TEXT` 建表成功。结论：该 store 在真实 PostgreSQL 部署上 `ensureSchema` 必抛 BadSqlGrammar——store-jdbc 迁移轨道明确支持 h2/postgresql/mysql 三方言（spec 04 §DB），本类「DDL 自含不耦合 store-jdbc 迁移轨道」的决策使其必须自行保证可移植性，此前只对 H2 成立。

修复（一词）：`spec_json CLOB` → `spec_json TEXT`——PG/MySQL 原生；H2 以既有 JdbcToolSetSpecStoreTest（H2 内存库）回归验证。补 `PostgreSqlToolSetSpecStoreTest`（Testcontainers，`disabledWithoutDocker = true`，与 store-jdbc 的 PostgreSqlStoresContractTest 同型）在真实 PG 上锁住方言回归。
