# Wayfinder Map — Buzhou 跨会话泄漏金丝雀检测（effort #528，E 会话第 29 轮）

> E 会话第 29 轮。勘察：CanaryGuardHook 是**注入检测**（密语进工具数据，
> 工具结果再现密语=间接注入）——**跨会话泄漏检测**空白：A 会话上下文里
> 的专属金丝雀若出现在 B 会话回复中 = 跨会话污染（共享缓存串话/历史
> 泄漏——单实例多租户隔离的探针）。thinkst canarytokens/honeytoken 思想。

## Destination

`guard.leak.SessionCanaryRegistry`：`plant(sessionId)` → 专属金丝雀令牌
`BUZHOU-LEAKCANARY-<8hex(sha256(sessionId|salt))>`（确定性——同会话重
取同令牌；salt 实例注入）；`detect(observerSessionId, text)` → 扫描文本
中**属于其他会话**的令牌 → LeakedFrom 列表（from=令牌主会话）+ 计数；
注册表有界 LRU 256（717 容量守卫同思想——tag 有界纪律）。诚实边界：
检测依赖令牌原样出现（模型改写/截断不保——概率探针非隔离机制）；与
CanaryGuardHook（注入检测）语义正交。

## Notes

- 号段：spec 528 / T809–810 / impl-431。
- 借鉴源：thinkst canarytokens / honeytoken（诱饵值泄漏即信号）。

## Out of scope

- 自动隔离会话；模糊/语义匹配；跨进程注册表。

## Tickets

- [x] [T809 令牌种植与跨会话侦测](../tickets/T809-session-canary-registry.md)
- [x] [T810 有界注册表与计数](../tickets/T810-session-canary-bounds.md)
