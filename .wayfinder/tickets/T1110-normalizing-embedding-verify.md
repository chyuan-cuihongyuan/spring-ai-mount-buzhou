---
id: T1110
title: 嵌入归一化装饰器验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1109]
created: 2026-09-13
---

## Question

归一正确性（点积=余弦）/计数口径/输入不突变如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 5 轮 = effort #804）：NormalizingEmbeddingModelTest 7 例——单位范数+index 保持+分量精确值/点积与 EmbeddingProvider.cosine 对照 1e-6/已归一跳算计数/零向量透传/embed(Document) 路径/输入数组不突变/双参 fail-fast。
