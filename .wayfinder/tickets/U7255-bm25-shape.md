---
id: U7255
title: U 会话 U28 Bm25Ranker 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

相关性排序怎么防长文刷分？（spec 7027 / effort #7027 / U28）

## Resolution

**Bm25Ranker（core/metrics）**：IDF 饱和+TF 饱和+长度归一；b 开关效应语义钉住；幂等替换静默核。
