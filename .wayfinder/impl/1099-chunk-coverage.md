# 1099 — 语义切片索引覆盖读面

**What to build:** SemanticChunkIndex 增量 coverageStats()（indexedUries/totalChunks/maxChunksPerUri/largestUri）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] SemanticChunkIndex 覆盖读面（byUri 只读遍历）
- [x] SemanticChunkIndexCoverageTest 三测
- [x] spec 1447 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SemanticChunkIndexCoverageTest'` 3/3 绿。
