# 875 — SemanticSkillRanker 排序分布深化

**What to build:** 静态三计数 + RankerDistStats + distStats()/resetDistForTest() + 分布测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数落点（rank 入口/跳过短路径/嵌入失败）
- [x] RankerDistStats 嵌套 record + distStats() + resetDistForTest()
- [x] SemanticRankerDistTest（正常/单候选/空 hint/失败/reset 五测）
- [x] spec 1136 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test -Dtest='SemanticRankerDistTest'` 全绿。commit 见本轮 `feat(skills)` 提交。
