---
id: T2173
title: 工具 schema 健康审计（ToolSchemaHealthAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 36 轮：工具 schema 裸奔审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ToolArgsValidator permissive 跳过策略使坏 schema=裸奔；裸奔规模无审计面。

形状裁决：ToolSchemaHealthAudit 纯函数（core/exec）——analyze(List<ToolCallback>)→Report 四态分桶（VALID/MISSING/UNPARSEABLE/NOT_OBJECT）+findings 有界 16+bypassRatio 派生（-1 哨兵）；分桶口径与校验器跳过条件严格同源（properties/required/type 全缺=裸奔）；纯只读。

Out of scope：深度质量；装配 fail-fast；builder 拦截说明入档。
