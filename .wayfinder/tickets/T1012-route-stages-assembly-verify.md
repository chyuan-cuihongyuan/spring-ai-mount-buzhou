---
id: T1012
title: 路由阶段标签 yml 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1011
created: 2026-09-13
---

## Question

双声明过滤生效？缺省零变化？<2 路 fail-fast？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 31 轮）：① MockEnvironment 双键 → filter 生效；② 缺省 → 原权重；③ 过滤后 1 路 → BuzhouConfigurationException；④ 全模块回归绿。
