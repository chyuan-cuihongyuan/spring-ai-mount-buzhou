# 846 — 死信重投成功率读数

> 来源：H 会话第 47 轮 = effort #846 / [T1193](../../.wayfinder/tickets/T1193-deadletter-redelivery-stats.md) / [T1194](../../.wayfinder/tickets/T1194-deadletter-redelivery-stats-verify.md) / impl 599。
> 借鉴：sidekiq retry set（扩散轮）。

## Problem

死信重投（若配置）没有结果统计：重投是在恢复还是持续失败、成功率多少——重投策略调参缺依据。

## Solution

`DeadLetterRedeliveryStats`（core.webhook，纯记账）：record(success)——attempts/successes/successRate（空尝试 0）+连续失败 streak（成功清零）；喂点=重投路径装配侧。

## 兼容性

纯新增；WebhookDeadLetter/WebhookEventForwarder 零变更。

## 诚实边界

不执行重投；全局计数（per-event 分账自建实例）；内存有界。
