---
id: T1656
title: 冒烟补全轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1655
created: 2026-09-15
---

## Question

J 会话第 98 轮：冒烟补全如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（两新增冒烟测试绿）：定向 `mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 绿。
