---
id: T1873
title: R34 选题——金丝雀候选限流放行与配额窗口语义（CanaryQuotaExhaustedTest）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 34 轮：金丝雀候选级限流（candidateLimiter acquireOrThrow）的放行/耗尽两态语义如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 34 轮 = effort #1233 / spec 1233 / impl 934 续）：

1. **放行态**：RateLimit RPM=10 额度内 → 双轮金丝雀照常直达备模型（候选级 acquireOrThrow 每轮放行，主模型零调用——金丝雀直达语义）；FIFO 出队顺序断言（首轮 from-canary / 次轮 from-canary-2，ScriptedChatModel 队列语义）。
2. **耗尽态语义实证（R34 打点）**：RPM=1 时次轮 acquireOrThrow 抛 ModelRateLimitExceededException(model=primary, dimension=RPM)——InMemoryRateLimitBackend 的窗口为全局限流（非按模型分键），耗尽后主路同窗也受限——该语义边界如实入档（候选级限流 ≠ 按模型独立配额，是全局限流窗在金丝雀候选上的投影）。
3. **边界**：耗尽降级 e2e 断言因全局限流窗语义暂不成立（主路同窗也受限），以放行态+窗口语义实证收口；按模型独立配额属 ModelRateLimiter 增强域（后续轮）。
