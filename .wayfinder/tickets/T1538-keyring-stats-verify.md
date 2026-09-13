---
id: T1538
title: 签名验钥分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1537
created: 2026-09-14
---

## Question

J 会话第 42 轮：验钥分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（KeyRingStatsTest，KeyPairGenerator 生成测试钥对）：rotate 后 activeVersion/rotations 同步；verifyKey 已知版本非 null 且 attempt 计数；未知版本返 null 计 miss；低于 minVerifyVersion 计 miss；rotate 版本重复/回退 IllegalArgumentException 回归；fresh（无钥环）verifyKey 恒 null。定向 `mvn -pl buzhou-guard test -Dtest='KeyRingStatsTest'` 绿 + 既有审计签名回归绿。
