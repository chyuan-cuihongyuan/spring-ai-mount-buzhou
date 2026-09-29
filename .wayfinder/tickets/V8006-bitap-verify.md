---
id: V8006
title: V 会话 V3 BitapSearch 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8005]
created: 2026-09-29
---

## Question

V3 合同怎么逐一验绿？（spec 8002 / effort #8002 / V3）

## Resolution

**验证通过**：三测全绿——手锚+63/64 位宽边界；300 随机 vs
indexOf 圣像；fail-fast（null/空/超位宽）。
