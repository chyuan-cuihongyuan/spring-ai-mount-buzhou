---
id: V8031
title: V 会话 V16 SipHash24 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

哈希 flood DoS 怎么破？（spec 8015 / effort #8015 / V16）

## Resolution

**SipHash24（core/crypto）**：64 位密钥 SipRound 2 压缩
4 终化的 keyed PRF 哈希；官方向量钉死实现；非加密承诺
明示。
