---
id: T1604
title: 沙箱版 run_command 执行分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1603
created: 2026-09-15
---

## Question

J 会话第 74 轮：SandboxRunStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SandboxRunStatsTest，桩 launcher 骨架——见既有 SandboxRunCommandTool 测试）：送达 → runs=1；空命令/黑名单/workdir/timeout 各桶；守恒 calls = runs + 五拒绝桶；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='SandboxRunStatsTest'` 绿 + 既有 SandboxRunCommandTool 回归绿。
