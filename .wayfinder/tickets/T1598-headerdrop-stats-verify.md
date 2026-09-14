---
id: T1598
title: http_request 受控头丢弃显形的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1597
created: 2026-09-15
---

## Question

J 会话第 71 轮：headerDrops 补桶如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（HttpToolStatsTest 增用例）：带 Host+Connection 双受控头请求本地回环 → headerDrops=2 且请求照常送达（successes 同增——丢弃不影响请求）；既有断言零回归。定向 `mvn -pl buzhou-tools -am test -Dtest='HttpToolStatsTest'` 绿 + 回归绿（worktree 隔离）。
