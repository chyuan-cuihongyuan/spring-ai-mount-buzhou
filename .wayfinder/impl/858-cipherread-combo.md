# 858 — cipher×readBack 组合测试轮

**What to build:** CipherReadBackComboTest——加密回读联动 + ReadRangeStats 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CipherReadBackComboTest（联动/守恒/隔离三测）
- [x] spec 1106 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='CipherReadBackComboTest'` 全绿。commit 见本轮 `test(spill)` 提交。
