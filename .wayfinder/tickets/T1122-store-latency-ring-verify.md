---
id: T1122
title: 存储延迟环形读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1121]
created: 2026-09-13
---

## Question

分位/挤老/透传/异常路径如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 11 轮 = effort #810）：StoreLatencyRingTest 7 例——1..100 分位 50/95 精确+total 5050/138 灌入挤老后 P50=74 P95=132 精确账/非法输入忽略/操作名封顶+recorded 含截断首笔/装饰器三方法透传计数/异常也计时且照抛/fail-fast。
