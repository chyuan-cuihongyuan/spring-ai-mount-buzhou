---
id: T2312
title: Javadoc 收口的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2311
created: 2026-09-15
---

## Question

M 会话第 34 轮：收口如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：四模块编译绿（guard/store-jdbc/store-redis/resilience）+ 门测试（CoreApiJavadocCoverageTest）零回归；纯注释零行为。
