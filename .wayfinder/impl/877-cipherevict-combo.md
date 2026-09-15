# 877 — 加密溢出×逐出组合测试轮

**What to build:** CipherEvictComboTest——加密溢出→逐出链双 stats 各自计数一致 + 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CipherEvictComboTest（溢出/逐出/守恒/隔离四测）
- [x] spec 1213 + README 行

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='CipherEvictComboTest'` 全绿。commit 见本轮 `test(spill)` 提交。
