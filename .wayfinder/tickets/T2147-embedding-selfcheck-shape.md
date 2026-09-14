---
id: T2147
title: Embedding 质量自查探针（EmbeddingSelfCheck）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 24 轮：embedding 换模型回归哨兵的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：EmbeddingProvider SPI 无质量自查面；R13 维度漂移题当时换出——本轴一并覆盖（dimension 显形）。

形状裁决：EmbeddingSelfCheck 纯函数（core/spi）——内建确定性合成句对（相似对×2+无关对照×2）+probe→ProbeReport 序判定（cos(sim)>cos(dis) 逐对+minMargin>0 派生 orderHolds）+dimension 显形；**序判定免绝对阈值**（绝对余弦随模型不可移植——口径核心）；语义词包替身测试（关键词维确定性）。

Out of scope：台账化；阈值配置；检索端到端。
