---
id: T1558
title: 命令黑名单拦截判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1557
created: 2026-09-15
---

## Question

J 会话第 51 轮：CommandBlacklistStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CommandBlacklistStatsTest）：默认黑名单危险命令（rm -rf /）→ matched=1；安全命令（ls）→ allowed=1；空白/null → allowed 桶（未拦截口径）；混合调用守恒 checks = matched + allowed；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='CommandBlacklistStatsTest'` 绿 + 既有 CommandBlacklist 回归绿（worktree 隔离应对共享树竞争）。
