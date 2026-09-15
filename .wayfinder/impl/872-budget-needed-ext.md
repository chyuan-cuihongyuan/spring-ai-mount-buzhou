# 872 — 预算 needed 判定分布并入

**What to build:** BudgetClampStats 尾参追加 neededTrue/neededFalse + 接线 + 测试适配。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] neededTrue/neededFalse 两落点（needed 判定处）
- [x] record 尾参追加 + stats()/resetForTest() 同步
- [x] 测试适配 + 新增 needed 分布用例
- [x] spec 1134 + README 行

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='BudgetClampStatsTest'` 全绿。commit 见本轮 `feat(memory)` 提交。
