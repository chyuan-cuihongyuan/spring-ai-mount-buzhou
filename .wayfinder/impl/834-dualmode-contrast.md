# 834 — 双档 run_command 对账组合测试轮

**What to build:** DualModeRunContrastTest——timeout=0 双档对照 + 各自守恒 + reset。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DualModeRunContrastTest（对照/守恒/reset 三测）
- [x] spec 1082 + README 行

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='DualModeRunContrastTest'` 全绿。commit 见本轮 `test(tools)` 提交。
