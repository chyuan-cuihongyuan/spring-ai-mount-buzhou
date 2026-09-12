---
id: T1016
title: 契约套件接入 store-jdbc 的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1015
created: 2026-09-13
---

## Question

JdbcSessionStateStore 过九项契约（尤其 CAS 语义——JDBC 条件单语句真原子）？H2 无 Docker CI 可跑？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 33 轮）：① H2StoresContractTest 新方法全绿（9/9）；② 既有 H2 契约测试零回归；③ store-jdbc 模块套件绿。
