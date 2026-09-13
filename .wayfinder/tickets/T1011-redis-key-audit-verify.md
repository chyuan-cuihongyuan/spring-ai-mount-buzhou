---
id: T1011
title: Redis 键命名空间碰撞审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1010]
created: 2026-09-12
---

## Question

审计发现与守卫谓词如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 6 轮 = effort #705）：①audit 断言三族 Finding 存在且 key/conflictWith 精确（obs:spev:spans、obs:event:spans、lease:a:seq）；②isSafeSessionId 通过/拒绝清单；③定制前缀审计形状不变。buzhou-store-redis 全模块零回归（C 会话排除集）。
