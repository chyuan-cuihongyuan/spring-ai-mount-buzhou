---
id: T2273
title: 工具调用瞬断重试（F1 功能缺口）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 13 轮：spec 05「运行期瞬断重试」承诺（design-incompleteness F1）如何落地？

## Resolution

**用户常设授权 AFK（可推翻）**

spec 05 承诺：独立配置默认 0 次、指数退避 1s/2s/4s 上限 3 次、IO 瞬断白名单、单调用任务内部重试不额外占许可。**形状修正（动工时查重发现）**：RetryingToolCallback 装饰器已存在（spec 133 / B 会话 #95 幂等重试 + spec 302 / C 会话 #302 重试预算接线）——F1 真实残留是**自动装配通道缺失**（既有装饰器靠宿主手动包装，幂等契约归声明方）。修正形状：

① IdempotentToolRetryHolder（Holder 模式）：buzhou.core.tool-transient-retry.enabled=true 声明即启用——@BuzhouTool.idempotent=true 或 idempotent-overrides 白名单的工具在会话装配期自动包既有 RetryingToolCallback（HookedToolCallback 内层：hook 只见逻辑调用一次）；
② 既有 RetryPolicy 加 transientOnly 档（默认 false 语义零变化；通道传 true）：瞬断白名单 isTransient（IO/超时族 + 类名启发，cause 链三层防包装漏判）——非瞬断（参数/业务类）零重试原样上抛，spec 05 白名单推演落地；
③ max-attempts clamp 由 RetryPolicy 校验、initial/max-backoff 透传；未装配 = 装配不包零变化。
