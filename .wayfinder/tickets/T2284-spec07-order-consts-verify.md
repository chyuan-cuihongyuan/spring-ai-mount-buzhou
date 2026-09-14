---
id: T2284
title: spec 07 回写与序位常量化的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2283
created: 2026-09-15
---

## Question

M 会话第 18 轮：回写与常量化如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-resilience,buzhou-spill test` 绿（180+384 用例零回归——序位常量与原字面量同值，advisor 链序行为逐位不变）；spec 07 文档改写三处（纯文档）。
