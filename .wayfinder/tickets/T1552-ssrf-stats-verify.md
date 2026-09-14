---
id: T1552
title: SSRF 守卫判定分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1551
created: 2026-09-14
---

## Question

J 会话第 48 轮：SsrfGuardStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SsrfGuardStatsTest）：公网主机（example.com）→ dnsAllowed=1；放行清单主机直通 → allowlisted=1；空主机 → emptyHostRejects=1；不存在域名 → dnsRejects=1；内网地址（127.0.0.1）→ blockedRejects=1；混合调用后守恒 checks = totalAllowed + totalRejects；resetForTest 归零。定向 `mvn -pl buzhou-tools -am test -Dtest='SsrfGuardStatsTest'` 绿 + 既有 SsrfGuard 回归绿。
