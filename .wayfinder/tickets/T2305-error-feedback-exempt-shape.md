---
id: T2305
title: 批预算错误反馈豁免（spec 1526 即时补强）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2303
created: 2026-09-15
---

## Question

M 会话第 30 轮：批级预算截断是否应豁免错误反馈？

## Resolution

**用户常设授权 AFK（可推翻）**

是。错误反馈（ToolErrorFeedback 结构化纠错信号——"错误即反馈"统一通道 spec 05/T16 的产物）是模型自纠的关键输入且通常很短：截断它省不了多少预算却毁纠错。形状：applyBatchBudget 截断候选跳过 isErrorFeedback 项（全部候选均为错误反馈的极端批按序截——预算语义保留）；R29 的即时补强（同谱系小步）。
