---
id: T1877
title: R32 选题——金丝雀路径 e2e 直测（adviseCallCanary/degradeFromCanary/canary-selected 事件）
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

K 会话第 32 轮：金丝雀路径（canaryEnabled + 权重确定性路由、金丝雀终态失败链序回退、canary-selected 事件）如何 e2e 补测？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（K 会话第 32 轮 = effort #1231 / spec 1231 / impl 934）：

1. **补测面（3 用例）**：canary 路由成功（权重全给 secondary → 回复来自备模型 + canary-selected 事件）；金丝雀终态失败链序回退主模型（switched 事件 from=secondary to=primary）；canary-selected 事件 payload 钉 model+sessionId。
2. **形态**：CanaryPathEndToEndTest——FallbackChainEndToEndTest 多模型 harness 复用（Fallback record canaryEnabled+weights 确定性路由）。
3. **边界**：不改主代码；candidateLimiter 限流拒绝分支（需限流器装配）留后续。
