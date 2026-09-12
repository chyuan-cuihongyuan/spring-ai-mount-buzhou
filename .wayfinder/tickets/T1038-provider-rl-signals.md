---
id: T1038
title: 供应商限流头前瞻读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

限流韧性全事后（429 才 Retry-After）——x-ratelimit 前瞻余量头加解析原语吗？自动降速吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 20 轮 = effort #719 / spec 719 / impl 619）：`ProviderRateLimitSignals` 纯静态——parse(HttpHeaders)→Signals(remaining/limit requests+tokens 可空+reset Duration 秒/复合/HTTP-date)+request/tokenUtilization（1−余/限，缺→NaN）+pressureLevel（NONE/MEDIUM/HIGH 0.8/0.95）；全 null-safe fail-safe；无头 empty()。单响应快照口径；advisor 接线与自动降速归消费端（OpenAI x-ratelimit 思想）。
