# 809 — 作业死信台账

> 来源：H 会话第 10 轮 = effort #809 / [T1119](../../.wayfinder/tickets/T1119-job-dead-letter.md) / [T1120](../../.wayfinder/tickets/T1120-job-dead-letter-verify.md) / impl 562。
> 借鉴：sidekiq dead set（≈13K star）。
> 换题注记：原 R10 题不可路由事件计数需侵入分发主路径——启用备选池 S8。

## Problem

DelayedJobQueue 一次性语义下异常作业「吞+failedCount」即消失：哪个作业键在反复失败、最后一次异常是什么——无明细可勘（失败即刻火化，无停尸间）。

## Solution

`JobDeadLetterLog`（core.concurrent）+ DelayedJobQueue 可选观察者：

- **挂接**：`new DelayedJobQueue(clock, (jobKey, err) -> log.recordFailure(...))`——null/旧构造零变化；观察者异常隔离（不炸调度线程）。
- **台账**：环形明细 64（jobKey/atMillis/errorType/message 截 200，挤最老）+ 按键聚合封顶 64（count/lastErrorType，超限 truncated）+ totalFailed。
- **快照**：recent 新→旧、byKey 次数降序、全只读。
- **不重投**：重投是提交方域（submit 同 key 替换语义即天然重投锚）。

## 兼容性

DelayedJobQueue 仅加构造器与 catch 内观察者调用——既有签名/行为零变化（3 例既有测试回归绿）。

## 诚实边界

一次性语义无自动重试（与 sidekiq dead set 的 retry 差异如实）；内存有界重启清零；明细与聚合独立封顶。
