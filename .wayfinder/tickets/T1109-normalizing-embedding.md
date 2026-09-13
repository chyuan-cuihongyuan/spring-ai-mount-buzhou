---
id: T1109
title: 嵌入归一化装饰器的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

L2 归一放装饰器还是消费方？已归一/零向量语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 5 轮 = effort #804 / spec 804 / impl 557）：`NormalizingEmbeddingModel`（721 同模式）——逐条 L2 归一+副本语义；ε=1e-4 内跳算计数、零向量透传计数；三计数面只读；双路径 call+embed(Document)。
