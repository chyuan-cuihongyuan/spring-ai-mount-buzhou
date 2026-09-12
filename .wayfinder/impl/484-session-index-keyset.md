# 484 — 会话索引 keyset 游标分页

**What to build:** SessionIndexQuery.cursor + CANONICAL_ORDER/afterCursor + 三实现接线（JDBC SQL 下推）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] spi 扩展（七参兼容 + codec 自校验）
- [x] InMemory/JDBC/Redis 三实现（Redis 平局序统一收口）
- [x] 内存 3 用例 + JDBC SQL 用例绿 + 双 store 契约零回归
- [x] spec 631 + README 行

## Done

验证：`mvn -pl buzhou-core,buzhou-store-jdbc,buzhou-store-redis -am test` 绿。commit 见本轮 `feat(core)` 提交。
