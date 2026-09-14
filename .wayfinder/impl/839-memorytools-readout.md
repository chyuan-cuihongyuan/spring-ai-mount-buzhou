# 839 — memory 域双工具组合测试轮

**What to build:** MemoryToolsReadoutTest——交叉调用 + 双守恒 + 互不串账 + reset 独立。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] MemoryToolsReadoutTest（交叉/双守恒/隔离/reset 四测）
- [x] spec 1087 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='MemoryToolsReadoutTest'` 全绿。commit 见本轮 `test(memory)` 提交。
