---
id: T1622
title: J 系读面统一契约冒烟轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1621
created: 2026-09-15
---

## Question

J 会话第 83 轮：统一契约冒烟如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ReadoutContractSmokeTest，starter 聚合模块反射驱动）：15 读面全量冒烟——stats() 可调、record 组件全非负、resetForTest 后全零、二次 stats 稳定。定向 `mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 绿。
