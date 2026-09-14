# 1097 — 媒体摄入统计读面

**What to build:** MediaIntake 增量（intakes/bytesTotal/readBacks+per-MIME 直方封顶 16+stats/reset）+ 综合测。

**Blocked by:** None.

**Status:** done

- [x] MediaIntake 埋点（intake 单漏斗+readBack）
- [x] MediaIntakeStatsTest（综合计数/直方/reset）
- [x] spec 1445 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='MediaIntakeStatsTest'` 1/1 绿。
