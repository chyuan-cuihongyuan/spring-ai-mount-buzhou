---
id: T6081
title: R 会话 R41 MVCC 快照可见性的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

一致性读怎么读不阻塞写且读序一致？（spec 4040 /
effort #4040 / R41）

## Resolution

**MvccVisibility（core/transaction）**：Postgres 快照口径——
事务注册（begin/commit/abort fail-fast）+ snapshot 捕获
（xmax+在飞+中止集）+ 纯函数可见性判定（创建者四条 ∧
删除者四条）；repeatable read 免费成立。
