# 1071 — Redis 慢操作榜

**What to build:** RedisSlowOpLog 静态面（阈值/FIFO 32/水位/动态调整/reset）+ RedisMessageStore 三操作 finally 埋点 + 五测。

**Blocked by:** None.

**Status:** done

- [x] RedisSlowOpLog（store-redis，private 构造静态面）
- [x] append/load/findById 埋点（load 拆 loadSlow 保持计时边界干净）
- [x] RedisSlowOpLogTest 五测
- [x] spec 1418 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-store-redis -am test -Dtest='RedisSlowOpLogTest'` 5/5 绿。
