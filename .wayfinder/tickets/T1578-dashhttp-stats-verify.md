---
id: T1578
title: Dashboard HTTP 状态分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1577
created: 2026-09-15
---

## Question

J 会话第 61 轮：DashboardHttpStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DashboardHttpStatsTest，骨架见既有 DashboardHttpServer 测试）：正常请求 → ok；无 token 访问 → authRejects；坏参数 → badRequests；未知路径 → notFounds；守恒 requests = 八桶和；resetForTest 归零。定向 `mvn -pl buzhou-observe-dashboard -am test -Dtest='DashboardHttpStatsTest'` 绿 + 既有 Dashboard 回归绿。
