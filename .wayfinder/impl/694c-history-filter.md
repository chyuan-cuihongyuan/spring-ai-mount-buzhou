# 694 续 — gate 历史按数据集过滤读面

**What to build:** EvalGate.historyOf（数据集精确匹配过滤投影）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] historyOf
- [x] GateHistoryFilterTest（过滤精确/序保持/fail-fast/无匹配空表/914 零变化）
- [x] spec 956 + README 行（欠账累计 926–956）

## Done

验证：`mvn -pl buzhou-core test -Dtest=GateHistoryFilterTest` 全绿。commit 见本轮 `feat(core)` 提交。
