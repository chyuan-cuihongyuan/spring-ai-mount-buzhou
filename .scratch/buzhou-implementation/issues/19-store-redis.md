# 19 — Redis 存储实现

**What to build:** buzhou-store-redis 过同一套 SPI 契约测试（Testcontainers Redis）；Lua/MULTI 原子批实现 unit-of-work；key 布局与 TTL 策略符合 08 spec；轻量 KV 部署场景可用。

**Blocked by:** 03

**Status:** done（实现 62ab5d0；buzhou-store-redis 五 SPI 全实现：message（RPUSH LIST + msgid 索引，load 内存排序）、summary（INCR 单调版本 + ZSET 索引）、state（per-key HASH + Lua CAS 比价 DEL+SREM，免 Lua 解析 JSON）、lease（Lua fencing：EXISTS 判占用 → INCR token → HSET+PEXPIRE；renew/release owner+fencingToken 双校验；steal 无条件取新 token；PEXPIRE 毫秒 TTL 自然到期）、observability（per-span HASH upsert + 会话/全局 span ZSET + event STRING + 会话/per-span event ZSET；listSessionSummaries 经全局 sessions ZSET 逆序分页 + 内存聚合 first/last activity/turnCount/spanCount/SESSION 属性袋）。UnitOfWork = 每事务独占连接 MULTI/EXEC（store 经 ThreadLocal 路由入队，exec 原子提交/discard 回滚，独占连接规避共享连接 MULTI 线程串入）；CAS/lease 走 Lua eval 原子。客户端 Lettuce（Boot BOM 管理版本）。key 布局/TTL/UoW 边界已回写 spec 08「buzhou-store-redis」节并收口推演 #9。AutoConfiguration 装配归 ticket 20。）

- [x] 契约套件在 Redis 实现上全绿（双轨：RedisStoresContractTest 基于 jedis-mock 进程内 hermetic 跑全 11 项契约 + UoW 原子两项，本机全绿；RedisStoresTestcontainersTest 基于 Testcontainers redis:7-alpine 跑真实 Redis，disabledWithoutDocker=true 本机无 Docker 跳过、CI 跑。本环境 Docker 不可用，真实 Redis 契约以 Testcontainers 在 CI 验收）
- [x] unit-of-work 经 Lua/MULTI 原子提交/回滚有测试（unitOfWorkCommitsAtomicallyOnSuccess + unitOfWorkRollsBackAllWritesOnFailure：append+put 在事务内，成功 exec 后双写可见、抛异常 discard 后双写皆无；UoW 用 MULTI/EXEC，CAS/lease 用 Lua——spec 08「Lua/MULTI 原子批」口径）
- [x] 文档写明 Redis 语义边界（过期/持久化注意项）（spec 08「buzhou-store-redis」节：message/summary/state/span/event 不设 TTL 依赖 AOF/RDB，lease PEXPIRE=租约 ttl，snapshot 可配 TTL；明示 Redis 崩溃未配 AOF/RDB 数据丢失、与 JDBC 事务持久性差异；UoW 独占连接 MULTI、事务内忌读-改-写）
