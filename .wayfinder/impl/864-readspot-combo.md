# 864 — readRange×Spotlight 组合测试轮

**What to build:** ReadRangeSpotlightComboTest——溢出占位标记段 + 包裹回读幂等。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ReadRangeSpotlightComboTest（标记段/幂等两测）
- [x] spec 1112 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='ReadRangeSpotlightComboTest'` 全绿。commit 见本轮 `test(spill)` 提交。
