---
id: T1142
title: 护栏豁免登记面验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1141]
created: 2026-09-13
---

## Question

过期惰性失效/续期/封顶/隔离如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 21 轮 = effort #820）：GuardExemptionRegistryTest 5 例——全生命周期+机制主体双隔离+撤销幂等/999 过界+惰性只计一次/续期 reason 覆盖+grantedTotal=2/封顶 64 精确+overflow 拒/脏参数五形态+快照 until 降序。
