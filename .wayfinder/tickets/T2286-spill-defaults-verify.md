---
id: T2286
title: spill 默认值单一事实源的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2285
created: 2026-09-15
---

## Question

M 会话第 19 轮：默认值收口如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-spill test` 绿（180 用例零回归——默认值等值，构造路径行为逐位不变）。
