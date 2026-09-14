# 809 — skill_search 搜索判定读面

**What to build:** SkillSearchTool 静态五计数（calls/hits/misses/parseRejects/blankQueryRejects）+ 嵌套 SkillSearchStats + stats()/resetForTest() + 命中/零结果/坏 JSON/空 query/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] 五计数落点（入口/命中非空/零结果/catch/空 query）
- [ ] SkillSearchStats 嵌套 record + stats() + resetForTest()
- [ ] SkillSearchStatsTest（命中/零结果/坏 JSON/空 query/守恒/reset 六测）
- [ ] spec 1057 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-skills -am test -Dtest='SkillSearchStatsTest'` 全绿 + 既有 SkillSearchTool 回归绿。commit 见本轮 `feat(skills)` 提交。
