---
id: T1664
title: 冒烟清单第三扩展轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1663
created: 2026-09-15
---

## Question

J 会话第 102 轮：清单扩展如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（17 成员全量冒烟绿）。定向 `mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 绿。
