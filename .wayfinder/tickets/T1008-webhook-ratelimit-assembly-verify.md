---
id: T1008
title: webhook 限速 yml 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1007
created: 2026-09-13
---

## Question

burst 缺省 = ceil(rate)？limiter 语义零回归？装配直通后 defer 路径有效？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 29 轮）：① burst 缺省口径直调断言（rate=2.5 → burst=3）；② limiter 节流/回填既有用例零回归；③ forwarder 全模块回归绿。
