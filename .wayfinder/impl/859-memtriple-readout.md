# 859 — memory 三读面大组合测试轮

**What to build:** MemoryTripleReadoutTest——三读面交叉 + 各自守恒 + 互不串账 + reset 独立。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] MemoryTripleReadoutTest（交叉/三守恒/隔离三测）
- [x] spec 1107 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='MemoryTripleReadoutTest'` 全绿。commit 见本轮 `test(memory)` 提交。
