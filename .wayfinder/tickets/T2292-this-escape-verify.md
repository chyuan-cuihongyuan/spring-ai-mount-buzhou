---
id: T2292
title: this 逃逸修复与三裁定的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2291
created: 2026-09-15
---

## Question

M 会话第 23 轮：修复如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-observability test` 绿（105 用例——含异步管线既有 flush/close 语义测试零回归，惰性启动行为等价：首事件前无 drain 需求）；CLAUDE/裁定项纯文档。
