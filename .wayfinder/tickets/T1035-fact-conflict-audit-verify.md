---
id: T1035
title: 共享事实冲突审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1034]
created: 2026-09-12
---

## Question

冲突/重复判定语义如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 18 轮 = effort #717）：①同键异值双 owner CONFLICT+entries 全列；②同键同值异 owner DUPLICATE；③健康集零发现+计数正确；④空表零发现+null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
