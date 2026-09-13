# 694 — SessionIndexStore 契约校验套件

**What to build:** SessionIndexStoreContract（五项静态 verify）+ IndexContractAccessTest（内存实现接入）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionIndexStoreContract.verify 五项
- [x] IndexContractAccessTest
- [x] spec 945 + README 行（欠账累计 926–945）

## Done

验证：`mvn -pl buzhou-core test -Dtest=IndexContractAccessTest` 全绿。commit 见本轮 `feat(core)` 提交。
