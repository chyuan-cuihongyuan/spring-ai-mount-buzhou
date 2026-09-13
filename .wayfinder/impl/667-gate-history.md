# 667 — gate 判定环形历史读面

**What to build:** EvalGate 环形历史（GateDecision + HISTORY_CAPACITY=16 + history() 快照）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] GateDecision 环形记录 + history() 新→旧快照
- [x] GateHistoryTest（入史/封顶/快照不可变/判定零变化）
- [x] spec 914 + README 行（欠账累计 906–914 九行）

## Done

验证：`mvn -pl buzhou-core test -Dtest=GateHistoryTest` 全绿。commit 见本轮 `feat(core)` 提交。
