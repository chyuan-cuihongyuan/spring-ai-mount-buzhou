---
id: T1655
title: 冒烟补全轮（TodoTool/ToolSlowLog 纳入）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1653
created: 2026-09-15
---

## Question

J 会话第 98 轮：R83 在册的两个形状差异读面的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R83 在册的 TodoTool（actionStats 形状）与 ToolSlowLog（stats 实例面）两读面游离于统一冒烟之外——补全其独立冒烟（非反射直接调用，按各自形状断言非负）。

形状裁决：`ReadoutContractSmokeTest` 增两测试——todoActionStatsSmoke（TodoTool 实例 actionStats byAction 值非负）与 toolSlowLogSmoke（ToolSlowLog 实例 stats 组件非负）。零生产改动。
