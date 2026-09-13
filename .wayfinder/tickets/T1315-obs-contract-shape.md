---
id: T1309
title: ObservabilityStore 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 36 轮：spec 922/929/930 契约系列覆盖了 SessionStateStore/MessageStore/SessionLeaseStore——ObservabilityStore（span/event/快照三存储 + cursor 摘要列表）的契约套件是否有缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 36 轮 = effort #936 / spec 936 / impl 688）：扩散成立（契约系列收口最后核心 SPI）。落点 `ObservabilityStoreContract`（spi 包静态 verify 范式）八项检查——①saveSpans 后 spansOfSession 保序返回 ②saveEvents 后 eventsOfSession 保序 ③injectionSnapshot 写读一致 ④未知会话空读（span/event） ⑤未知 turnSeq 快照 empty ⑥跨会话隔离（s1 写入不影响 s2 读）⑦deleteSession 后读空且幂等 ⑧eventsOfSpan 按 spanId 过滤。配套内存实现接入测试（core 测试域）。
