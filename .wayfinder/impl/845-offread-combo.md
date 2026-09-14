# 845 — offload→readBack 双轴闭环组合测试轮

**What to build:** OffloadReadBackComboTest——溢出→回读闭环 + 双读面守恒 + 闭环对应。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] OffloadReadBackComboTest（闭环/双守恒/对应三测）
- [x] spec 1093 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='OffloadReadBackComboTest'` 全绿。commit 见本轮 `test(spill)` 提交。
