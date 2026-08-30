# impl-89 — outbox 前缀扫描 SPI

**What to build:** SessionStateStore.scanByPrefix（JDBC/Redis 下推）+ WebhookOutbox 四路径
改走前缀扫描——消除到期批扫全量读放大。

**Blocked by:** None

**Status:** done

- [x] SPI default（getAll 过滤）+ JDBC LIKE 下推 + Redis 键集合侧过滤
- [x] WebhookOutbox：seq 续起/due/deadLetters/pendingCount 全改 scanByPrefix
- [x] core/jdbc/redis 三模块全绿（webhook 6 用例回归背书）；spec 33 §C

## Done

commit：见 git log（impl-89）。
