---
id: T1546
title: fs 沙箱判定计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1545
created: 2026-09-14
---

## Question

J 会话第 45 轮：沙箱判定计数读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SandboxVerdictStatsTest，TempDir 骨架）：沙箱内解析 resolutions=1 violations=0；.. 逃逸 → SandboxViolationException + violations=1；空路径 → violation 计数；resolveForWrite 同口径；守恒 violations ≤ resolutions。定向 `mvn -pl buzhou-core test -Dtest='SandboxVerdictStatsTest'` 绿 + 既有 FileSandbox 回归绿。
