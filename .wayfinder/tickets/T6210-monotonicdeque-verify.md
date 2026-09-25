---
id: T6210
title: T 会话 T5 Monotonic Deque 单调队列的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6209]
created: 2026-09-25
---

## Question

T5 合同怎么逐一验绿？（spec 6004 / effort #6004 / T5）

## Resolution

**验证通过**：MonotonicDequeTest 五测全绿——500 点窗 7 暴力
扫圣像全程全等；升/降/全等边界；越窗弹出语义；双实例同
操作同 max 序列；fail-fast。
