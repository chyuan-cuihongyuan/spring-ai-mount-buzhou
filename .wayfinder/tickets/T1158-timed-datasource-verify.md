---
id: T1158
title: 计时 DataSource 验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1157]
created: 2026-09-13
---

## Question

计时/异常路径/委托完整性如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 29 轮 = effort #828）：TimedDataSourceTest 4 例——双 getConnection 计数/异常照记照抛/委托七法（unwrap 泛型修正 DataSource.class）/双参 fail-fast。
