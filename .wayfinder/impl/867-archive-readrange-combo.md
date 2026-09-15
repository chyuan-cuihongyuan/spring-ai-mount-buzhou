# 867 — 归档×readRange 生命周期组合测试轮

**What to build:** ArchiveReadRangeComboTest——溢出→归档→回读链 + ReadRangeStats 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ArchiveReadRangeComboTest（溢出/归档/回读/守恒/隔离五测）
- [x] spec 1119 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='ArchiveReadRangeComboTest'` 全绿。commit 见本轮 `test(memory)` 提交。
