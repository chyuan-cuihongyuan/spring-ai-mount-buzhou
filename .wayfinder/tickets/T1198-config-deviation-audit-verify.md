---
id: T1198
title: 配置默认偏离审计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1197]
created: 2026-09-13
---

## Question

偏离判定/无基线跳过/封顶典序如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 49 轮 = effort #848）：ConfigDeviationAuditTest 3 例——偏离+无基线跳过+率 0.5/封顶 37→32 典序/空值跳过空真。测试两处笔误修正（defaults 误放+负数键名）。
