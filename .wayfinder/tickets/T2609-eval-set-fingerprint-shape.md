---
id: T2609
title: 评测集内容指纹的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

评测集指纹的形状怎么裁决？（spec 1704 / effort #1704 / R5）

## Resolution

**静态纯函数 `EvalSetFingerprint`（core/eval）**：`of(items[, sensitivity])` =
规范形（\n 连接 UTF-8）SHA-256 → `sha256-<64hex>`；`OrderSensitivity` 闭集
ORDERED（默认，序即内容）/ UNORDERED（字典序排序后摘要）。null/空表稳定
常量指纹。借鉴 DVC / HuggingFace Datasets 数据集指纹。零摘要库依赖。
