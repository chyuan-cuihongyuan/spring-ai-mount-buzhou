---
id: T1684
title: readRange×Spotlight 组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1683
created: 2026-09-15
---

## Question

J 会话第 112 轮：回读包裹组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ReadRangeSpotlightComboTest，SpillModule 骨架 + core Spotlighting 直调）：溢出占位含标记段 + 包裹后回读幂等。定向 `mvn -pl buzhou-spill -am test -Dtest='ReadRangeSpotlightComboTest'` 绿。
