---
id: T891
title: 时间窗验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T890
created: 2026-09-12
---

## Question

时间窗语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（CircuitTimeWindowTest 3/3 + resilience 全模块 240/240 零回归）：

- timeWindow=60s：t0 四失败（<minCalls 不跳）→ 推 61s 陈旧出窗 → 一新失败只见 1 新样本（< minCalls）不跳闸。
- timeWindow=0（默认）：同序列 count 窗 5/5 照跳（零变化）。
- 窗内新失败照常积累跳闸（衰减不是豁免）。
