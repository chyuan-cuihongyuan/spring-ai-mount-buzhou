# 843 — 冒烟清单扩展轮

**What to build:** ReadoutContractSmokeTest 清单追加 ArchivePurgeJob/SpillCipher（13→17？实际 15）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 清单追加两成员 + 全量冒烟绿
- [x] spec 1091 + README 行

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ReadoutContractSmokeTest'` 全绿。commit 见本轮 `test(starter)` 提交。
