---
id: T6061
title: R 会话 R31 EIP-1559 基础费调节的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

配额定价怎么有界地随供需调节？（spec 4030 / effort #4030 / R31）

## Resolution

**Eip1559BaseFee（core/policy）**：EIP-1559 弹性费用市场——
每轮按用量 vs 目标调节基础费（满块涨空块跌），弹性钳制单步
≤ ±1/8 + 地板止跌 + BigInteger floor 精确；超弹性越界
fail-fast（机器性约束调用方保证）。
