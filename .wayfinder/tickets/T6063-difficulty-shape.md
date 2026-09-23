---
id: T6063
title: R 会话 R32 难度目标重定的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

难度目标怎么周期性有界重定不失锚不尖叫？（spec 4031 /
effort #4031 / R32）

## Resolution

**DifficultyRetarget（core/policy）**：Bitcoin retarget——窗式
重定（实际耗时 vs 目标耗时），实际钳到 [target/4, ×4] 单窗
≤ 4× 不尖叫；powLimit 封顶难度地板；重定块开新窗滚动；
倒流 fail-fast 确定性。
