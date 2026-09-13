---
id: T1291
title: webhook 限流器余量快照读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 21 轮：WebhookRateLimiter 有 deferredCount 但无令牌余量读面（TurnRateLimitHook 有 availableSnapshot 先例）——余量快照是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 21 轮 = effort #920 / spec 920 / impl 673）：缺口成立——「令牌还剩多少/桶多大/流速多少」不可见，运维无法区分「限流配置过低」与「突发超预期」。落点 `WebhookRateLimiter.snapshot()`（synchronized 与 tryAcquire 同锁——强一致）：`record Snapshot(double tokens, double capacity, double refillPerSecond, long deferred)`（refill 时点修正后的实时余量——refill 语义复用既有私有逻辑）。纯读面零行为变化；deferredCount 既有面保留。
