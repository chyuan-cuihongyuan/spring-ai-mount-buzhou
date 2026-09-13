# 683 — 租约契约接入 Redis store

**What to build:** RedisLeaseContractTest（jedismock 九项契约）+ ACQUIRE_SCRIPT 幂等重入修复。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ACQUIRE_SCRIPT 同 owner 重入分支（PEXPIRE 续期返回原 token）
- [x] RedisLeaseContractTest 九项全过
- [x] spec 930 + README 行（欠账累计 906–930 二十五行）

## Done

验证：`mvn -pl buzhou-store-redis -am test -Dtest=RedisLeaseContractTest,RedisStoresContractTest` 全绿。commit 见本轮 `fix(store-redis)` 提交。
