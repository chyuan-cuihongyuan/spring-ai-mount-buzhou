---
id: T2845
title: 混沌预算门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

混沌实验的爆炸半径怎么有界？（spec 1822 / effort #1822 / R23）

## Resolution

**Netflix Chaos Monkey/Chaos Toolkit 思想纯裁决 `ChaosBudgetGate`
（core/exec）**：Window(start≤end 契约，空窗口合法) + decide 三态
MAY_RUN/OUT_OF_BUDGET/FORBIDDEN_WINDOW（窗口优先于预算——安全第一）+
usage 使用账（spent/remaining 钳 0/burnRatio -1 哨兵/exhausted，超支照实
入账不外泄负值）。与 ChaosMonkeyHook（执行器）构成闸+执行对。

