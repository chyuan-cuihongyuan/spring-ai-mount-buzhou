---
id: T2109
title: 会话 id 熵审计（SessionIdEntropyAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 5 轮（换题轮）：会话 id 质量审计的形状与熵口径选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：原题指标基数守卫与 spec 160/T513（tag 基数守卫，Loki cardinality limit）全撞——换入 R19 备选题（grep -i entropy 零命中）。会话 id 宿主供给无质量审计面。

形状裁决：`SessionIdEntropyAudit` 纯函数静态面——单 id `audit`（字母表下界估计：观测字符类保守求和+去重异类符号；bits=length×log2(alphabet) nanoid 同款口径）+ 四档闭集（INVALID/WEAK<64/MODERATE/STRONG≥112≈UUIDv4）+ 批量 `auditAll` 四桶 Summary；只读不裁决不接线。

Out of scope：生成器接线；Shannon 频率熵；id 模式黑名单。
