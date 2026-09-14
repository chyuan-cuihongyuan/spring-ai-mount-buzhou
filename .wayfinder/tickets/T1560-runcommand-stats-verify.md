---
id: T1560
title: run_command 执行结果分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1559
created: 2026-09-15
---

## Question

J 会话第 52 轮：RunCommandStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（RunCommandStatsTest，TempDir workdir 骨架）：正常命令 → exits=1（非零 exit 命令也入 exits——送达口径断言）；空命令 → blankRejects=1；黑名单命令 → blacklistRejects=1；不存在 workdir → workdirRejects=1；timeoutSeconds=0 → timeoutParamRejects=1；sleep 超时 → timeouts=1；混合守恒 attempts = exits + canceled + timeouts + totalRejects；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='RunCommandStatsTest'` 绿 + 既有 RunCommandToolTest 回归绿（worktree 隔离）。
