---
id: T6052
title: R 会话 R26 重启强度的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6051]
created: 2026-09-23
---

## Question

R26 合同怎么逐一验绿？（spec 4025 / effort #4025 / R26）

## Resolution

**验证通过**：SupervisorRestartIntensityTest 五测全绿——恰满 3/3
不越；第 4 次越限闩锁 + 窗滑走后仍保持；滑窗淘汰（now−t ≥
window 全出）+ 新窗重算 1/3；reset 新纪元从头计；畸形三型
fail-fast。首版滑窗保留语义手推滑错已按 ≥window 淘汰修正。
