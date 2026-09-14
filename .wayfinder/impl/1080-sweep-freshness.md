# 1080 — 保留清扫新鲜度追踪器

**What to build:** RetentionSweepFreshness implements Consumer<RetentionSweepReport>（listener seam 零侵入：计数/末次/间隔水位/失败分桶）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] RetentionSweepFreshness（core/retention，opt-in 实例面，调用方时钟）
- [x] RetentionSweepFreshnessTest 五测
- [x] spec 1427 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='RetentionSweepFreshnessTest'` 5/5 绿。
