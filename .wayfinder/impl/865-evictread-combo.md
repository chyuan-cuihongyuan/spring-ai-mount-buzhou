# 865 — evict×readRange 逐出复活组合测试轮

**What to build:** EvictReadBackComboTest——逐出→回读→再逐出链 + 双 stats 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EvictReadBackComboTest（链路/双 stats/守恒三测）
- [x] spec 1114 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='EvictReadBackComboTest'` 全绿。commit 见本轮 `test(spill)` 提交。
