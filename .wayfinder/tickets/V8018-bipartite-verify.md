---
id: V8018
title: V 会话 V9 BipartiteChecker 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8017]
created: 2026-09-29
---

## Question

V9 合同怎么逐一验绿？（spec 8008 / effort #8008 / V9）

## Resolution

**验证通过**：三测全绿——偶圈/奇圈/树手锚；200 随机图 vs
奇圈枚举暴力圣像判定全等；sides 互斥覆盖+fail-fast。
