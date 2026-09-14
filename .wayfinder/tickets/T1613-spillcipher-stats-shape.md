---
id: T1613
title: Spill 加解密读面（SpillCipherStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1611
created: 2026-09-15
---

## Question

J 会话第 79 轮：spill 安全域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SpillCipher（AES-256-GCM 溢出内容加解密）零计数——加解密调用分布与失败不可见。加密面操作量是安全配置真实生效的直接证据（AWS KMS 操作审计思想：加密服务自身的操作分布是配置验证基座）。

形状裁决：`SpillCipher` 内静态 `AtomicLong` 四计数——encryptCalls/encryptFailures（加密侧）/ decryptCalls/decryptFailures（解密侧）；四计数独立双组（加解密独立操作，失败通常伴随异常外溢——计数在异常外溢前落桶，不改变异常语义）；嵌套 `record SpillCipherStats` + `stats()` + `resetForTest()`。静态面理由同族先例；encrypt/decrypt 返回与异常语义逐位不变。

Out of scope：按内容大小分桶；密钥轮换计数（MAGIC 版本面另轴）。
