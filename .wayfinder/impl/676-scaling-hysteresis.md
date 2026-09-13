# 676 — 扩缩容建议缩容滞回

**What to build:** BulkheadScalingAdvisor stabilizeWindows 重载（缩容滞回计数 + 默认 1 兼容）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] stabilizeWindows 构造重载 + 滞回计数
- [x] ScalingHysteresisTest（默认 1 零回归/滞回保持/连续满足回落/扩容即时）
- [x] spec 923 + README 行（欠账累计 906–923 十八行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ScalingHysteresisTest` 全绿。commit 见本轮 `feat(core)` 提交。
