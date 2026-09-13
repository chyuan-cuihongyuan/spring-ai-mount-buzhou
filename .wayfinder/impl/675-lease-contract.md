# 675 — SessionLeaseStore 契约校验套件

**What to build:** SessionLeaseStoreContract（九项语义检查静态 verify）+ LeaseContractAccessTest（内存实现接入示例）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionLeaseStoreContract.verify 九项
- [x] LeaseContractAccessTest
- [x] spec 922 + README 行（欠账累计 906–922 十七行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=LeaseContractAccessTest` 全绿。commit 见本轮 `feat(core)` 提交。
