---
id: T1495
title: 加密消息存储操作计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 23 轮：加密消息存储操作计数读面（信封加密 ops 可视性）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 23 轮 = effort #1022 / spec 1022 / impl 775）：缺口成立——EncryptingMessageStore（spec 333 消息静态加密装饰器）append 加密 / load·findById 解密 / 旧明文与已信封**双方向透传**全程零计数：加密实际覆盖了多少消息、透传了多少（迁移期明文残留与幂等跳过混在一起）不可见。落点 core.crypto：实例级 encrypted/decrypted/passthrough 三 AtomicLong（toCarrier/fromCarrier 单点各计；**解密失败照抛不计数**——完整性优先语义不变）+ 嵌套 record `CryptoStoreStats(encrypted, decrypted, passthrough)` + `stats()`。实例级；嵌套类型不动 API 快照；行为逐位不变。
