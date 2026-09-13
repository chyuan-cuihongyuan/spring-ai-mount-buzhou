# 688 — ObservabilityStore 契约校验套件

**What to build:** ObservabilityStoreContract（八项静态 verify）+ ObsContractAccessTest（内存实现接入）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ObservabilityStoreContract.verify 八项
- [x] ObsContractAccessTest
- [x] spec 936 + README 行（欠账累计 926–936）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ObsContractAccessTest` 全绿。commit 见本轮 `feat(core)` 提交。
