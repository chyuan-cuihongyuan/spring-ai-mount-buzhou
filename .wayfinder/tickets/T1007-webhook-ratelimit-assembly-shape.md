---
id: T1007
title: webhook 限速 yml 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

WebhookRateLimiter（spec 718）只有编程面——yml 声明式入口缺失。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 29 轮 = effort #728 / spec 728 / impl 531）：webhookEventForwarder bean（已有 env 参数——105/533 同模式）读 `buzhou.webhook.rate-limit-per-second`（>0 启用）与 `rate-limit-burst`（缺省 ceil(rate)）→ setRateLimiter 直通。缺省零变化。
