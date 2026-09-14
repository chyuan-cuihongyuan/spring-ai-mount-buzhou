---
id: T1614
title: Spill 加解密读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1613
created: 2026-09-15
---

## Question

J 会话第 79 轮：SpillCipherStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillCipherStatsTest，既有密钥骨架——128 字节 base64 或 fromBase64Key 合法 32 字节）：encrypt → encryptCalls=1；decrypt 往返 → decryptCalls=1；坏密钥构造 → 构造期抛出（不计数）；resetForTest 归零。定向 `mvn -pl buzhou-spill -am test -Dtest='SpillCipherStatsTest'` 绿 + 既有 SpillCipher 回归绿。
