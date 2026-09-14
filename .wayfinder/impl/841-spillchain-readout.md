# 841 — spill 域 offload+evict 生命周期组合测试轮

**What to build:** SpillLifecycleReadoutTest——溢出→逐出链路 + 双读面守恒 + 互不串账。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SpillLifecycleReadoutTest（溢出/逐出/守恒/隔离四测）
- [x] spec 1089 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SpillLifecycleReadoutTest'` 全绿。commit 见本轮 `test(spill)` 提交。
