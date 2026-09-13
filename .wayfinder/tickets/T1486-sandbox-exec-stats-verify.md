---
id: T1486
title: 沙箱执行结果分桶读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1485
created: 2026-09-14
---

## Question

J 会话第 18 轮：执行结果分桶读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SandboxExecStatsTest，桩 delegate 定果——CommandResult 构造可控）：正常完成 executions=1；delegate 报 timedOut → timeouts=1 且归因 TIMEOUT；超限输出 → outputTruncations=1 且归因 OUTPUT；既有归因不被覆盖（delegate 已给 reason 时保持）；混合守恒 executions ≥ timeouts + outputTruncations（两者可叠加于同一次执行）。定向 `mvn -pl buzhou-guard test -Dtest='SandboxExecStatsTest,LimitedCommandSandboxTest'` 绿。
