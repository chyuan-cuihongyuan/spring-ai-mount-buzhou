---
id: T867
title: 幂等键验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T866
created: 2026-09-12
---

## Question

幂等键传播如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（IdempotencyKeyPropagationTest 3/3 + core 全模块 1755/1755 零回归）：

- 同批两调用：各见含自身 callId 的键（互不相同）。
- 同 callId 两次派发（重试模拟）：键恒同。
- 无会话绑定独立使用：anon 前缀诚实降级。
- 既有 exec 断言（baggage/deadline/coalescer）零回归——per-call 拷贝不破坏共享键。
