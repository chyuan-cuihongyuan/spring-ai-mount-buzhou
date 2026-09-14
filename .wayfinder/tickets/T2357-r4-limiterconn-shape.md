---
id: T2357
title: R4 http_request per-host 并发上限的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2356
created: 2026-09-15
---

## Question

N 会话第 4 轮：单 host 过载防护选哪种形状——拒绝（limit_conn）还是排队等待？

## Resolution

选 **拒绝**。排队等待会占住虚拟线程与轮次 deadline（等待无上界即变相挂死轮次）；
Nginx limit_conn 对超限直接 503 的语义在工具调用场景翻译为「失败转文本给模型，
模型自然学会错峰」。闸挂在 SSRF 校验之后（被拒地址不占名额）。CAS 计数器实现，
归零条目留存（量级论证见 spec——SSRF 放行域约束 host 集合）。
