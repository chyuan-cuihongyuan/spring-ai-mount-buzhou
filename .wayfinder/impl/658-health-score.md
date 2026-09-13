# 658 — 健康加权评分读面

**What to build:** BuzhouHealthScore 纯函数（UP=100/UNKNOWN=50/DOWN=0 算术平均 + 分档常量）+ ScoreReport record + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouHealthScore.compute + ScoreReport record
- [x] BuzhouHealthScoreTest（全 UP/带 DOWN/UNKNOWN 中性/分档边界/空集）
- [x] spec 905 + README 行 + API 快照再生

## Done

验证：`mvn -pl buzhou-core test -Dtest=BuzhouHealthScoreTest` 全绿。commit 见本轮 `feat(core)` 提交。
