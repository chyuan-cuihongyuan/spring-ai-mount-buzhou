# 728 — webhook 投递限速 yml 装配

> 来源：G 会话第 29 轮 = effort #728（D 会话装配轮模式）/ [T1007](../../.wayfinder/tickets/T1007-webhook-ratelimit-assembly-shape.md) / [T1008](../../.wayfinder/tickets/T1008-webhook-ratelimit-assembly-verify.md) / impl 531。

## 背景

WebhookRateLimiter（spec 718）只有编程面——`buzhou.webhook.rate-limit-per-second` 声明式入口缺失。

## 目标

- webhookEventForwarder bean（已有 env 参数）读 `rate-limit-per-second`（>0 启用）与 `rate-limit-burst`（缺省 ceil(rate)）→ setRateLimiter 直通。
- 缺省（无属性）逐字节不变。

## 测试

limiter 语义既有面（spec 718）+ 装配参数解析直调用例（burst 缺省=ceil(rate)）；全模块回归。

## 兼容性

缺省零变化。
