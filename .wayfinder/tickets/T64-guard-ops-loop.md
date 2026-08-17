---
id: T64
title: guard · 审计链/密钥/策略运维闭环
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

guard 的运维面如何闭合？需裁决：① AuditChain 持久化（存储介质、append 失败语义）+ AutoConfig 接线；② SigningKeyRing 版本化轮换（keyVersion 嵌记录、旧钥只验不签、minVerifyVersion、KeyProvider SPI 文件加载，Vault Transit 语义）；③ AuditChainVerifier 独立校验工具 + VerificationReport + sessionHash 发布/nightly 重放；④ policy 热加载（PolicySource etag 快照原子替换 + provenance 进 PolicyDecision，OPA bundle 语义）；⑤ SandboxLimits（timeout/memory/maxOutputBytes/netAllowlist + CommandResult truncated/killedReason，@deno/sandbox 语义）+ DenoSandbox --version 探测缓存。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §guard-9..10**：AuditRecordStore SPI（JDBC append-only + InMemory 有界环形）持久化；guard 自动装配接线（默认随 guard 开，无密钥降级哈希链 + WARN）；SigningKeyRing（keyVersion 嵌记录、rotate 原子切换、旧钥只验不签、minVerifyVersion、KeyProvider 文件加载——Vault Transit 语义）；AuditChainVerifier 独立校验（VerificationReport 定位首断点）+ sessionHash 发布 + nightly 重放（注记）；PolicySource etag + PolicyRefresher PT30S 原子快照替换 + provenance 进 PolicyDecision（OPA bundle 语义）+ PolicyGateHook 装配；SandboxLimits（timeout/maxOutputBytes/可选 memory）+ CommandResult truncated/killedReason + Deno 探测缓存。
