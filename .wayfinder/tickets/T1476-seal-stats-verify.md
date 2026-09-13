---
id: T1476
title: 加密封存操作生命周期计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1475
created: 2026-09-14
---

## Question

J 会话第 13 轮：seal/open 生命周期计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（复用既有 EncryptedSessionExport 测试骨架——EnvelopeCipher 测试实现/固定密钥环）：seal 后 sealed=1；open 成功 opened=1；非封缄串 open → openRejected=1；换钥开 → openRejected=1（密钥失配路径）；篡改载荷 → openRejected=1；正确操作 stats 与行为零变化回归。定向 `mvn -pl buzhou-core test -Dtest='EncryptedSessionExportStatsTest,EncryptedSessionExportTest'` 绿。
