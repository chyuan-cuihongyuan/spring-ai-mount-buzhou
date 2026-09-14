---
id: T2364
title: R7 虚拟线程 pinning 修复的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2363
created: 2026-09-15
---

## Question

N 会话第 7 轮：如何验收？

## Resolution

CanaryConcurrencyTest 双 latch 并行断言：两臂工具各自进入阻塞段（锁内执行时代码
第二臂进不来 → 超时失败）；计数守恒（各臂恰一次）+ view 读数。回归
CanaryToolCallbackTest 7 用例零变化。审计结论（17 组）入档 spec 1606。
