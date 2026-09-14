---
id: T2359
title: R5 响应缓存 stale-if-error 的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2358
created: 2026-09-15
---

## Question

N 会话第 5 轮：模型故障期的可用性兜底——过期条目保留救场还是失败直抛？

## Resolution

选 **宽限保留 + 失败救场**（Varnish grace / RFC 5861 stale-if-error）。纯直抛把
「几秒前的正确答案」变成硬错误，可用性无谓损失；但救场必须有时限（stale-window）
且只在上游失败时发生（正常路径 get 仍 miss——新鲜度语义不被稀释）。无救场条目时
异常照抛：救场是显式策略不是错误过滤器。流式不救场（聚合语义复杂，独立裁决）。
