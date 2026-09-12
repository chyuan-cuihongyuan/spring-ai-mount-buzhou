---
id: T1010
title: Redis 键命名空间碰撞审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

Redis 键布局实测存在结构性潜伏碰撞（spev/event 保留段、lease 冒号后缀）——做审计面吗？改键形状吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 6 轮 = effort #705 / spec 705 / impl 605）：`RedisKeyLayoutAudit` 静态原语——audit(prefix) 结构性对抗模拟产出 Finding（三族碰撞：RESERVED_SEGMENT/COLON_SUFFIX_TRICK/SPAN_INDEX Clash）+reservedSegments() 读数+isSafeSessionId 摄入守卫谓词（无冒号/非保留段/无 glob，从严供选用）。不改键形状（迁移语义归大版本）。fsck 思想+E R12 教训制度化。
