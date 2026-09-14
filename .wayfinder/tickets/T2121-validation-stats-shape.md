---
id: T2121
title: 工具入参校验读数（ToolArgsValidator 静态面）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 11 轮：校验拒绝分布读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：H R21 聚合让位的下半轴开放——ToolArgsValidator 纯静态无读面；validate 三调用点（入参×2+结果 schema×1）。

形状裁决：计数内置 validate()（静态入口自动全覆盖）——validations/accepted+守恒 rejectedDerived；七错误桶**标记单源**（MARK_* 常量 check() 与分桶共用）+桶非互斥如实入档；无可校验结构不入账；嵌套 ValidationStats+resetValidationStatsForTest；错误文本改常量拼接内容同串（逐位兼容）。

Out of scope：工具名 tag；调用点分面；错误结构化返回。
