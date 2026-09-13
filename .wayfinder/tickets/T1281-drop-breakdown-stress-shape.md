---
id: T1281
title: EventDropBreakdown 并发压测的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 16 轮：spec 900 的丢弃分类计数（ConcurrentHashMap+LongAdder）在多调用方并发 enqueue 下是否需要压测验证？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 16 轮 = effort #915 / spec 915 / impl 668）：需要——enqueue 是多调用方路径（会话可多线程并发 chat/事件），countDrop 的「总量 incrementAndGet + 分类 computeIfAbsent/increment」两步非原子，并发下守恒不变量（ΣbyReason == dropped）依赖 LongAdder 正确性。落点 `EventDropBreakdownConcurrencyTest`（G r47 压测同模式）：parallel stream 4000 并发 enqueue 恒溢出场景（容量 2 + 慢消费），断言 ① ΣbyReason == stats().dropped()（守恒）② dropped == enqueued-交付数（不丢计数）③ 分类值集 ⊆ 预期原因集 ④ 多分发器实例互不串账。压测本身即热路径验证（1-2 秒内完成）。
