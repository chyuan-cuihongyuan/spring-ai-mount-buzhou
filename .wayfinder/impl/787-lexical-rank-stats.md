# 787 — 词法排序生效计数读面

**What to build:** LexicalSkillRanker runs/reordered 两计数 + 嵌套 RankStats + stats() + ClasspathSkillScanner 骨架测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] runs/reordered 计数埋点（早返不计）
- [x] RankStats 嵌套 record + stats()
- [x] LexicalRankStatsTest（命中变序/空问法不计/对账）
- [x] spec 1034 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-skills test -Dtest='LexicalRankStatsTest'` 全绿。commit 见本轮 `feat(skills)` 提交。
