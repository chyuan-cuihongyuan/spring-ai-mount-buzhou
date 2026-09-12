---
id: T1021
title: 全局 holdout 层验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1020]
created: 2026-09-12
---

## Question

层语义（跨实验一致排除）如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 11 轮 = effort #710）：①holdout=100 全 unit 全实验 null+__holdout__ 计数；②holdout=0 与既有行为一致；③同 unit 跨两实验一致排除（层语义）；④>100 构造拒绝。buzhou-core 全模块零回归。
