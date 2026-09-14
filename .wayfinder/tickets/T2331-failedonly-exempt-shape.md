---
id: T2331
title: FAILED_ONLY 占位批预算豁免的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2305
created: 2026-09-15
---

## Question

M 会话第 44 轮：FAILED_ONLY 反馈策略与批预算（spec 1522 族）组合语义如何保障？

## Resolution

**用户常设授权 AFK（可推翻）**

组合测试（FAILED_ONLY × 预算 80 × 失败+大成功对）实证：占位文本（54 字符）非错误反馈格式被截到 0——模型丢失成功信号。修：FAILED_ONLY_PLACEHOLDER_PREFIX 常量化（生成/豁免共用单源）+ applyBatchBudget 豁免条件纳入（元信息非数据，豁免语义与错误反馈一致）。
