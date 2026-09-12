---
id: T991
title: 错误签名已知问题静默标记的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

ErrorSignatures 的 top-N 被已知问题（上游依赖抖动、已立案缺陷）长期霸榜——新错误族被挤出视野。Sentry resolved/muted issues 语义怎么映射？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 21 轮 = effort #720 / spec 720 / impl 523）：ErrorSignatures 增静默标记面——`mute(sig)`（进有界集合 MUTED_CAP=64，超限拒绝并返回 false）、`unmute(sig)`、`mutedSignatures()` 不可变视图；`top(n)`/`top(kind,n)` **排除 muted**（计数照常累计不丢数据——snapshot() 原样含 muted，原始事实可查）；`reset()` 连带清空 muted（新纪元语义）。定位：静默是**读面降噪**不是数据删除——已知问题的存在性仍可从 snapshot 证实。借鉴 getsentry/sentry muted/resolved issues。
