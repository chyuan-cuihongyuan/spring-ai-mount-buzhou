# 682 — 租约契约接入 H2/JDBC store

**What to build:** H2LeaseContractTest（H2 内存库 JdbcSessionLeaseStore 过九项契约）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] H2LeaseContractTest（九项全过断言 + 失败清单可读）
- [x] spec 929 + README 行（欠账累计 926–929）

## Done

验证：`mvn -pl buzhou-store-jdbc -am test -Dtest=H2LeaseContractTest` 全绿。commit 见本轮 `test(store-jdbc)` 提交。
