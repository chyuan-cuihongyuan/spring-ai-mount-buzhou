---
id: T1531
title: 提示词注册表解析分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 39 轮：提示词注册表解析分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 39 轮 = effort #1039 / spec 1039 / impl 791）：缺口成立——InMemoryPromptRegistry（spec 401 提示词版本化注册表）resolve/resolveVersion 解析全程零计数：解析多少次、命中多少、**未命中多少**（提示词名拼错/标签缺失=配置错误信号，与 R17 技能解析未命中分轴——那轴是技能、本轴是提示词模板）不可见。落点 core/prompt：实例级 resolutions/hits/misses 三 AtomicLong + 嵌套 record `PromptResolutionStats(attempts, hits, misses)`（守恒 attempts == hits + misses）+ `resolutionStats()`；计数收敛到公共解析核心（resolve 双入口与 resolveVersion 共用 findVersion，不重复计）。实例级；嵌套类型不动 API 快照；解析返回值逐位不变。
