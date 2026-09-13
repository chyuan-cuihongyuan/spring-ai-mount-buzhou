# 786 — token 估算调用量与总量读面

**What to build:** CharHeuristicTokenEstimator 静态三计数 + 嵌套 TokenEstimateStats + stats()/resetForTest() + 确定性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] estimateCalls/batchCalls/totalEstimatedTokens 三计数
- [x] TokenEstimateStats 嵌套 record + stats()/resetForTest()
- [x] TokenEstimateStatsTest（单文本/批量/null/归零）
- [x] spec 1033 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='TokenEstimateStatsTest'` 全绿 + 既有 TokenEstimator 回归绿。commit 见本轮 `feat(core)` 提交。
