---
id: T871
title: 每连接并发上限验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T870
created: 2026-09-12
---

## Question

并发上限语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（McpConnectionConcurrencyTest）：

- limit=1 同 server 两工具并发：第二调用阻塞至第一释放（完成顺序可断言）。
- 不同 server 互不影响（各自独立信号量）。
- 默认不设：两调用真并发完成（零行为变化）。
- 上限变化对既有条目不追溯（新条目生效——诚实边界入档）。
