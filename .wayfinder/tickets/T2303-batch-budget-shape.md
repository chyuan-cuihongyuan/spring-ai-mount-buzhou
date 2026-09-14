---
id: T2303
title: 批级工具结果回喂预算的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 29 轮：单工具限幅（ToolResultLimiter）之上的批内总量护栏如何设？

## Resolution

**用户常设授权 AFK（可推翻）**

缺口实证：N 个工具各自限内但合计巨大的回喂仍会撑爆上下文（单工具维度管不住批维度）。形状：HarnessToolCallingManager.applyBatchBudget——批总量超预算时按响应长度降序贪心截大者（保留尽量多的小结果完整——比平均截断信息保留更多），截断件带标记+保留量+可重查指引，指标 buzhou.tools.batch-truncated；BatchResponseBudgetHolder 进程级（EvalPrune 同款），buzhou.core.tool-batch-response-budget > 0 声明即启用，HarnessAssembler 构造期拾取注入 per-session manager；0 = 关零行为（默认）。
