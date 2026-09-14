---
id: T1625
title: Skill 管理操作读面（SkillAdminStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1623
created: 2026-09-15
---

## Question

J 会话第 85 轮：skills 管理面的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SkillAdminApi（DB Skill 管理 CRUD + 状态迁移）五操作零计数——管理面操作分布不可见，治理审计无基座。GitHub repo admin API statistics 思想。

形状裁决：`SkillAdminApi` 内静态 `AtomicLong` 五计数——creates/updates/publishes/disables/deletes（五操作成功返回处独立计数）；嵌套 `record SkillAdminStats` + `stats()` + `resetForTest()`。口径诚实：五操作类型独立计数不设统一守恒（同 R63 独立口径）；校验异常抛出不计数。静态面理由同族先例；五方法返回与异常语义逐位不变。

Out of scope：按 name 分桶（名称配置面）；资源上传计量（另轴）。
