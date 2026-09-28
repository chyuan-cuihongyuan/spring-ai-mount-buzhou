---
id: U7275
title: U 会话 U38 FastModPow 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

模幂怎么免连乘？（spec 7037 / effort #7037 / U38）

## Resolution

**FastModPow（core/crypto）**：指数二进制位扫描平方累积 O(log)；long 安全域 fail-fast；费马钉子+BigInteger 圣像。
