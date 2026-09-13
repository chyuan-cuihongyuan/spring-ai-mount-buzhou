# 674 — TurnDeadline 软截止窗口读法

**What to build:** TurnDeadline.withinSoftWindow / softDeadlineAt 值对象读法 + 校验 + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] withinSoftWindow + softDeadlineAt
- [x] SoftWindowTest（窗内/窗外/已到期/哨兵/非法参数/既有零回归）
- [x] spec 921 + README 行（欠账累计 906–921 十六行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=SoftWindowTest` 全绿。commit 见本轮 `feat(core)` 提交。
