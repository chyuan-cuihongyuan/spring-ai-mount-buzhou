---
id: T1271
title: AIMD 自适应批量 yml 装配的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 11 轮：spec 907 的 setAdaptiveBatchEnabled 只有编程面——声明式部署（yml 用户）不可用。装配缝如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 11 轮 = effort #910 / spec 910 / impl 663）：D/G 会话装配轮模式（spec 105 include-types 同法——Binder 根绑定直读 env）：`webhookEventForwarder` bean 构造点直读 `buzhou.webhook.adaptive-batch`（boolean，缺省 false）→ `setAdaptiveBatchEnabled`。缺省（无此键）逐字节不变；false 显式声明 = 编程面关（同语义）。测试：装配单测断言 Binder 读取分支（true→启用读数波动 / 缺省→32 恒定）。
