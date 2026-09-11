---
id: T866
title: 工具幂等键的派生口径与传播形态
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Stripe 的 X-Idempotency-Key：客户端为每次逻辑操作生成键随请求发送，服务端按键去重——重试不再重复扣款。buzhou 工具调用的幂等键应如何派生与传播？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 9 轮 = effort #600 / spec 608 / impl 461）：

1. 键 = `sessionId:callId`（会话缺省 `anon`）——callId 是模型对本逻辑调用的稳定标识，重试/合并执行下恒同键。
2. 传播 = ToolContext：批共享 context 之上 per-call 浅拷贝加键（`buzhou.idempotency.key`）+ 静态读取器 `idempotencyKeyOf`（308 deadline / 337 baggage 同模式）。出站 HTTP 工具读键作上游幂等头——是否采用由工具实现决定（框架不强制不改协议）。
3. 不落库不签名：键是纯派生值（确定性拼接），无需持久化；与 spec 133 幂等重试分类正交（133 决定「要不要重试」，本键让「重试安全」）。
