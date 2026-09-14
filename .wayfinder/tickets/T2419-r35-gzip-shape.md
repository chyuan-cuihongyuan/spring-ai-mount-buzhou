---
id: T2419
title: R35 dashboard gzip 的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2418
created: 2026-09-15
---

## Question

N 会话第 35 轮：gzip 无条件压还是客户端协商？

## Resolution

选 **客户端协商 + 阈值**。HTTP 语义要求不得对未协商客户端压缩（老代理/
爬虫兼容）；512B 阈值下压缩头（~20B+CPU）倒挂。writeJson 单点改造——
全部 API 端点统一获得。
