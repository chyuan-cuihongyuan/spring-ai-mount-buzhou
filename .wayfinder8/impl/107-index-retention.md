# impl-107 — 索引 CLOSED 行保留

**What to build:** 过期 CLOSED/DELETED 行惰性淘汰（ACTIVE 永不扫），保留期可配。

**Blocked by:** None

**Status:** done

- [x] purgeOlderThan SPI default + 内存/JDBC/Redis 三覆写
- [x] SessionIndexObserver 1/64 概率清扫（≤256 条）+ configureRetention 注入（closed-retention）
- [x] 契约 purge 用例（ACTIVE 保护/未过期保护/limit）——core/jdbc/redis 全绿；spec 37 §C

## Done

commit：见 git log（impl-107）。
