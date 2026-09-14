# 699 续 — LeaderElector 契约校验套件

**What to build:** LeaderElectorContract（五项静态 verify）+ LeaderContractAccessTest（内存实现接入）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] LeaderElectorContract.verify 五项
- [x] LeaderContractAccessTest
- [x] spec 954 + README 行（欠账累计 926–954）

## Done

验证：`mvn -pl buzhou-core test -Dtest=LeaderContractAccessTest` 全绿。commit 见本轮 `feat(core)` 提交。
