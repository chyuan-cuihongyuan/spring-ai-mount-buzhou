---
id: T2437
title: R44 流式 PII 豁免的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2436
created: 2026-09-15
---

## Question

N 会话第 44 轮：流式豁免只做类型级还是扩展 SPI 带会话级？

## Resolution

选 **只做类型级**。StreamTextFilter SPI（chunk 流）无会话上下文——扩展
SPI 携带 sessionId 是 core 面变更（影响所有实现者），为一个豁免粒度不值；
类型级在 filter 创建时一次判定（每轮视图），成本零。会话级豁免对输出侧
（1627）已可用——用户想要会话级豁免走非流式路径。
