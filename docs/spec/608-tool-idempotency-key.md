# 608 — 工具调用幂等键传播

> 借鉴：[stripe/stripe-node](https://github.com/stripe/stripe-node) X-Idempotency-Key——逻辑操作级键随请求传播，重试由上游去重。
> 来源：F 会话第 9 轮 = effort #600 / [T866](../../.wayfinder/tickets/T866-idempotency-key-shape.md) / [T867](../../.wayfinder/tickets/T867-idempotency-key-verify.md) / impl 461。

## 背景

工具重试（spec 133 只读工具静默退避重试）与批内合并（spec 300）都会重复执行「同一逻辑调用」；若工具背后是计费/下单类上游 API，重复执行即重复副作用。Stripe 的解法是调用方生成稳定幂等键随请求发送。

## 目标

`HarnessToolCallingManager` 为每个逻辑工具调用派生幂等键 `sessionId:callId`，经 ToolContext（键 `buzhou.idempotency.key`）传播到工具实现；静态读取器 `idempotencyKeyOf`。

## 非目标

- 不强制工具使用（框架不改协议；出站 HTTP 工具自行决定是否设头）。
- 不落库/不签名（纯派生确定性值）。
- 与 spec 133 正交：133 决定要不要重试，本键让重试安全。

## 设计

- 派生：`(sessionId ?? "anon") + ":" + toolCall.id()`——callId 是模型对逻辑调用的稳定标识，重试/合并下恒同键。
- 传播：批共享 ToolContext 之上 per-call 浅拷贝加键（308 deadline / 337 baggage 同模式）。

## 测试

3 用例：同批各见其键 / 同 callId 跨派发恒同 / anon 缺省；core 全模块零回归。

## 兼容性

ToolContext 增键纯增量；既有 context 消费者（读特定键）不受影响。
