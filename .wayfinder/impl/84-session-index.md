# impl-84 — 会话索引与枚举（SessionIndexStore）

**What to build:** 五 store 之外的枚举/过滤查询面——生命周期自动维护（最终一致），
内存/JDBC（V3）/Redis 三实现 + 自动装配；未装配零影响。

**Blocked by:** None（T109 已闭合）

**Status:** done

- [x] SPI：SessionInfo / SessionIndexQuery / SessionIndexStore
- [x] SessionIndexObserver（onOpen/onTurnEnd/onClose 维护，wiring 工厂，异常只 WARN）+ core auto-config 自动接线
- [x] InMemorySessionIndexStore / JdbcSessionIndexStore（V3 迁移×3 方言）/ RedisSessionIndexStore（ZSET+STRING 独立连接）
- [x] jdbc/redis auto-config bean；迁移测试升 V3
- [x] 测试：core e2e 三用例 + H2 三用例 + Testcontainers 一用例——core 284 / jdbc 67 / redis 41 全绿
- [x] spec 30 新篇

## Done

commit：见 git log（impl-84）。
