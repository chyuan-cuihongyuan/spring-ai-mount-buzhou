# 855 — 双档读写四象限对照组合测试轮

**What to build:** DualModeRwSymmetryTest——两组装写读对称 + 双守恒 + reset。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DualModeRwSymmetryTest（两组装对称/守恒/reset 三测）
- [x] spec 1096（编号复用：组合系列随轮号顺延）——勘误：本 impl 对应 spec 1103

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='DualModeRwSymmetryTest'` 全绿。commit 见本轮 `test(tools)` 提交。
