# 857 — EpisodeLedger 双实例组合测试轮

**What to build:** EpisodeLedgerDualInstanceTest——跨实例累计 + 双守恒 + reset。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EpisodeLedgerDualInstanceTest（跨实例/双守恒/归零三测）
- [x] spec 1105 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='EpisodeLedgerDualInstanceTest'` 全绿。commit 见本轮 `test(memory)` 提交。
