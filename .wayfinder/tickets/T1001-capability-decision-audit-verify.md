---
id: T1001
title: 能力门决策审计读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1000]
created: 2026-09-12
---

## Question

审计面正确性如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 1 轮 = effort #700）：四用例——①环形覆盖（容量 2 记 3 deny → recent 最新 2 条、dropped=1、denied=3）；②admit 只计数+denyByModel 聚合；③null fail-fast+snapshot 不可变（防御拷贝）；④advisor 接线冒烟（缺 vision 请求异常照抛且 denied=1）。buzhou-resilience 全模块零回归（C 会话排除集）。
