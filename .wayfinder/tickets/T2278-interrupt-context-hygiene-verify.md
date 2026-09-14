---
id: T2278
title: 中断与异常上下文卫生轮的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2277
created: 2026-09-15
---

## Question

M 会话第 15 轮：卫生轮如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：隔离 worktree（HEAD 既有 CounterAtomicitySpreadTest 失败绕行——core install 跳测后单模块跑）`mvn -pl buzhou-spill,buzhou-mcp test` 绿——spill 180 用例（含异常文案断言若有）+ mcp 107 用例零回归；shutdown 中断分支编译验证（变量名避让外层 Entry）。
