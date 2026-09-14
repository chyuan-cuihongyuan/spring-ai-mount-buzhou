# 838 — 双守卫（黑名单+SSRF）组合测试轮

**What to build:** DualGuardReadoutTest——混合调用 + 双守恒 + 互不串账 + reset 独立。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DualGuardReadoutTest（混合/双守恒/串账隔离/reset 四测）
- [x] spec 1086 + README 行

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='DualGuardReadoutTest'` 全绿。commit 见本轮 `test(tools)` 提交。
