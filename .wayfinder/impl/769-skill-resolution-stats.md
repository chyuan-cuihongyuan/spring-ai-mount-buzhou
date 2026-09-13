# 769 — 技能解析未命中计数读面

**What to build:** DefaultSkillRegistry loads/resolved/notFound 三计数（load-only 口径）+ SkillResolutionStats record + resolutionStats() + 双路守恒测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数（load-only 口径——清单枚举路径不污染）
- [x] SkillResolutionStats record + resolutionStats()
- [x] SkillResolutionStatsTest（命中/幻觉名/守恒/枚举不计数）
- [x] spec 1016 + README 行 + API 快照增行

## Done

验证：`mvn -pl buzhou-skills test -Dtest='SkillResolutionStatsTest,SkillSearchToolTest'` 全绿。commit 见本轮 `feat(skills)` 提交。
