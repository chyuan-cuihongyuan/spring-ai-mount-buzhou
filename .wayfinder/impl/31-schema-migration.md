# 31 — stores · Schema 版本化迁移 + MySQL 幂等 + 恢复设施装配

**What to build:** JDBC 部署可升级、可重复启动、可多实例冷启动：自建轻量版本管理（版本表 + 有序迁移脚本 + 基线判定），MySQL 索引幂等化修复「第二次启动必失败」，PG advisory lock / MySQL GET_LOCK 防并发建表竞态；事件溯源日志与 Run 注册表的 JDBC 实现接线进 store 组合工厂。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] buzhou_schema_version 表 + V<n> 有序脚本机制（方言分目录）+ 启动期执行器
- [x] 基线判定：有表无版本行 → 标记基线不重跑；空库 → 全量建
- [x] 首个正式迁移演示加列路径（旧库 ALTER，如 reasoning_signature）
- [x] MySQL 索引幂等化；PG advisory lock / MySQL GET_LOCK 并发保护
- [x] JdbcToolCallLog / JdbcRunRegistry 进 store 组合工厂（二进制兼容：新组合形状，旧工厂 deprecated 保留）
- [x] Testcontainers：MySQL 连续两次启动均 green；旧 schema 基线升级用例；（gated）PG 并发建表不炸
