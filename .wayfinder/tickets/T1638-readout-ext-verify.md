---
id: T1638
title: 冒烟清单扩展轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1637
created: 2026-09-15
---

## Question

J 会话第 91 轮：清单扩展如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（清单 17 成员全量冒烟绿）：反射三性质对新增两成员同样成立。定向 `mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 绿。
