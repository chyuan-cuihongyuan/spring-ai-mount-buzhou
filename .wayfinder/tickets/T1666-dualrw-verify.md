---
id: T1666
title: 双档读写四象限对照组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1665
created: 2026-09-15
---

## Question

J 会话第 103 轮：双档读写对照如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DualModeRwSymmetryTest，TempDir 骨架）：两组装下写→读对称恒等 + 双守恒 + reset 隔离。定向 `mvn -pl buzhou-tools -am test -Dtest='DualModeRwSymmetryTest'` 绿。
