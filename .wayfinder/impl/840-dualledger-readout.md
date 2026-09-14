# 840 — memory 双台账组合测试轮

**What to build:** DualLedgerReadoutTest——交叉调用 + 双台账计数互不串账 + reset 独立。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] DualLedgerReadoutTest（交叉/互不串账/reset 独立三测）
- [x] spec 1088 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='DualLedgerReadoutTest'` 全绿。commit 见本轮 `test(memory)` 提交。
