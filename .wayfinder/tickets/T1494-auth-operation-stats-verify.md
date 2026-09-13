---
id: T1494
title: HITL 审批操作分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1493
created: 2026-09-14
---

## Question

J 会话第 22 轮：审批操作分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（AuthOperationStatsTest，InMemorySessionStateStore 直构——e2e 同款 approve/revoke 流程）：fresh 零值；approve → approved=1；reject → rejected=1（不写授权——isAuthorized 仍 false）；revoke → revoked=1 且 isAuthorized 转 false；便捷重载 approve 三参同计。定向 `mvn -pl buzhou-guard test -Dtest='AuthOperationStatsTest'` 绿 + 既有 GuardAuthApi 相关回归绿。
