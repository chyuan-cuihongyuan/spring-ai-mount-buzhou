---
id: T60
title: stores · Schema 版本化迁移与并发建表健壮性
type: grilling
status: closed
assignee: ""
blocked-by: [T55]
created: 2026-08-14
---

## Question

JDBC schema 演进路径如何建立？需裁决：① 自建轻量版本管理（schema_version 表 + 有序迁移脚本，Flyway 思想注记、零新依赖）vs 引入 Flyway（stars 达标？11.7K★ 需核验）的取舍；② MySQL 索引幂等修复；③ 既有库从「幂等建表」到「版本化」的首次迁移策略（基线版本判定）；④ 加列路径（ALTER 迁移脚本示例：reasoning_signature 等已存在的漂移）；⑤ PG 并发冷启动 advisory lock；⑥ JdbcToolCallLog/JdbcRunRegistry 装配接线。

## Resolution

**ratify（用户常设授权，可推翻）→ spec 13 §stores-5**：自建轻量版本管理（buzhou_schema_version 表 + 有序 V<n> 脚本 + 方言分目录，Flyway 注记不引入保持零依赖）；启动期并发保护（PG advisory lock / MySQL GET_LOCK）；基线判定（有表无版本行→标基线不重跑）；首个迁移演示加列路径；MySQL 索引全幂等化；JdbcToolCallLog/JdbcRunRegistry 接线进 store 组合工厂（二进制兼容演进）。
