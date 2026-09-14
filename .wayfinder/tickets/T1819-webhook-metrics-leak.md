---
id: T1819
title: Webhook 测试泄漏后台重试线程污染全局指标捕获（ToolDurationTimerTest 顺序扰动红）——R6 对账显形
type: task
status: open
assignee:
blocked-by:
created: 2026-09-15
---

## Question

R6 第二次全仓 verify（同 worktree、同提交点）显形新 flake：`ToolDurationTimerTest.toolDurationRecordedWithOutcomeTags` 红——CapturingMetrics 捕获到 `buzhou.webhook.failures` / `buzhou.webhook.dead-letter` 外来条目（WebhookEventForwarder 死信重试 WARN 同时段出现）。surefire 未配并行（顺序执行），泄漏从何而来、如何收口？

## Resolution

（待下轮收口：初判 = WebhookEventForwarder 测试的重试调度线程在测试类结束后仍存活（attempts=3 退避重试越界执行），向后续测试窗口内安装的全局 BuzhouMetricsHolder 实例注入指标；修复方向 = forwarder 测试 @AfterEach 显式关闭/await 其调度线程（消灭越界面），或全局 Holder 断言改过滤式——前者治本；单列票下轮修，本轮诚实入档不计入绿色声明。）
