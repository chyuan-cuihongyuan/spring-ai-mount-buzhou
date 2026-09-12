---
id: T1054
title: Redis 键审计健康面接线的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

705 审计无健康面——接线吗？DOWN 语义用不用？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 28 轮 = effort #727 / spec 727 / impl 627）：`RedisKeyLayoutHealth` implements BuzhouHealth——mechanism=redis-key-layout 恒 UP（findings 是数据——548 同口径）；details 聚合三族计数+保留段；装配随 BuzhouRedisStoreAutoConfiguration 条件继承+ConditionalOnMissingBean。
