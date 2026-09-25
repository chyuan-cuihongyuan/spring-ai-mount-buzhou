---
id: T6268
title: T 会话 T34 Elevator Scan 电梯扫掠的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6267]
created: 2026-09-26
---

## Question

T34 合同怎么逐一验绿？（spec 6033 / effort #6033 / T34）

## Resolution

**验证通过**：ElevatorScanTest 五测全绿——上行经典序钉住；
下行对称；磁头自身请求最先；重复幂等；越域 fail-fast。
