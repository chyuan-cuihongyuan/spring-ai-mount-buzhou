---
id: T1017
title: 评估项结果记忆化验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1016]
created: 2026-09-12
---

## Question

命中/失配/默认关三态如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 9 轮 = effort #708）：①同 key 二跑模型调用数 0 增+hits=1+`[MEMO]` 前缀+结果一致；②改 expected → 失配重跑+misses+回写；③默认关二跑调用数 ×2；④ERROR 项不缓存真实重跑。buzhou-core 全模块零回归（C 会话排除集）。
