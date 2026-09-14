# 1085 — Skill 管理操作读面

> 来源：J 会话第 85 轮 = effort #1085（[T1625](../../.wayfinder/tickets/T1625-skilladmin-stats-shape.md) / [T1626](../../.wayfinder/tickets/T1626-skilladmin-stats-verify.md) / impl 837）。借鉴：GitHub repo admin API statistics（管理面操作分布是治理审计基座）。skills 管理面首轴。

## Problem Statement

`SkillAdminApi`（DB Skill 管理 CRUD + 状态迁移）五操作零计数——create/update/publish/disable/delete 的操作分布不可见：管理面治理审计无基座（谁在什么时候高频上下架无从对账）。

## 目标

- `SkillAdminApi` 增量（skill/manage，静态面）：五 `AtomicLong`。
  - `creates` / `updates` / `publishes` / `disables` / `deletes`——五操作成功返回处独立计数。
- 嵌套 `record SkillAdminStats(...)` + `stats()` + `resetForTest()`。
- 口径诚实：五操作类型独立计数不设统一守恒（同 R63 独立口径）；校验异常抛出不计数（异常入口不入桶）。

## 兼容性

纯增量读面：create/update/publish/disable/delete 返回与异常语义逐位不变；静态面理由同 R46–R84 先例；无新配置项。

## Out of Scope

- 按 name 分桶（名称配置面）。
- 资源上传计量（另轴）。
