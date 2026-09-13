---
id: T1537
title: 签名验钥分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 42 轮：签名验钥分布读面（cert-manager/keyring ops 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 42 轮 = effort #1042 / spec 1042 / impl 794）：缺口成立——SigningKeyRing（审计签名密钥环）verifyKey 对**版本未知或低于 minVerifyVersion** 的验证请求静默返 null：退役钥/未知钥的验证尝试次数不可见——旧钥重放探测与 minVerifyVersion 配置合理性无水位。落点 buzhou-guard audit 包：实例级 verifyAttempts/verifyKeyMisses/rotations 三 AtomicLong + 嵌套 record `KeyRingStats(verifyAttempts, verifyKeyMisses, rotations, activeVersion, minVerifyVersion)` + `stats()` 快照。实例级；嵌套类型不动 API 快照；verifyKey/rotate 行为逐位不变（仅加计数）。
