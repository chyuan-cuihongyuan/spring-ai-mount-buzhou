---
id: T1104
title: Redis 大值审计验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1103]
created: 2026-09-13
---

## Question

归类/定级/排名/聚合如何证明？有界性与脏样本口径如何守住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 2 轮 = effort #801）：RedisValueSizeAuditTest 5 例——族归类 11 断言（含 null→other）/CRIT-WARN 定级+降序排名+低于阈值计入族聚合不进 top/Top32 封顶含边界序/脏样本三形态跳过+空真/阈值与 null fail-fast。buzhou-store-redis 绿（C 会话排除集）。快照门：1 新公共类型入档。
