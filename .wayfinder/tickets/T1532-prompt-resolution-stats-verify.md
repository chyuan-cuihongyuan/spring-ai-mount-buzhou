---
id: T1532
title: 提示词注册表解析分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1531
created: 2026-09-14
---

## Question

J 会话第 39 轮：解析分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（PromptResolutionStatsTest，InMemoryPromptRegistry 直构 + publish/label 骨架）：resolve(name) 命中 LATEST → hits=1；label 解析命中与未命中各计；resolveVersion 未知版本 → miss；守恒 attempts == hits + misses；fresh 零值。定向 `mvn -pl buzhou-core test -Dtest='PromptResolutionStatsTest'` 绿 + 既有 PromptRegistry 回归绿。
