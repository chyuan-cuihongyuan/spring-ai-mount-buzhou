# 509 — API 快照 diff 破坏性分级

**What to build:** SnapshotDiff 纯函数分类器（added/removed/breaking/gradeMessage）+ 快照门失败信息分级装配——门语义不变，处置指引分级。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SnapshotDiff（gradeDiff 纯函数 + breaking 只看 removed）
- [x] 快照门失败信息分级（破坏性清单前置 + 分类处置指引）
- [x] 三类 diff 分类用例 + 既有门零回归
- [x] spec 706 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test` 绿。commit 见本轮 `test(starter)` 提交。
