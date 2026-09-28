---
id: U7265
title: U 会话 U33 KahanSummator 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

浮点连加误差怎么压平？（spec 7032 / effort #7032 / U33）

## Resolution

**KahanSummator（core/metrics）**：低位移存被舍入位下一轮补回；误差 O(1)；double 域明示。
