---
id: T2395
title: R23 对账 NPE 修复的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2394
created: 2026-09-15
---

## Question

N 会话第 23 轮：R19 对账改动的 request()=null NPE（CounterAtomicitySpreadTest
挂，M 会话 spec1513 记档归属本会话）如何修？

## Resolution

**前置 null 防御**：request()/prompt()/instructions() 任一缺席跳过对账（测试
替身链路只带 response——对账本是纯观测旁路，缺席不记即诚实）。跨会话记档
承接闭环：并行会话发现的非己线缺陷按归属修复，流程自愈。
