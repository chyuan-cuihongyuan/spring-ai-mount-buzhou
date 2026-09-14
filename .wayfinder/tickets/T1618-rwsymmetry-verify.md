---
id: T1618
title: 读写对称守恒组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1617
created: 2026-09-15
---

## Question

J 会话第 81 轮：读写对称守恒如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ReadWriteSymmetryTest，TempDir 沙箱骨架）：ASCII/中文/混合三内容对称恒等 + 双侧守恒 + reset 隔离。定向 `mvn -pl buzhou-tools -am test -Dtest='ReadWriteSymmetryTest'` 绿 + file 工具回归绿。
