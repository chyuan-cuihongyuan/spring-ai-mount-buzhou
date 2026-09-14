# 856 — todo×http 跨工具工作流组合测试轮

**What to build:** TodoHttpComboTest——交叉调用 + 双读面独立 + reset 独立。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TodoHttpComboTest（交叉/独立/隔离三测）
- [x] spec 1104 + README 行

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='TodoHttpComboTest'` 全绿。commit 见本轮 `test(tools)` 提交。
