# 866 — SemanticChunkIndex 操作读面

**What to build:** SemanticChunkIndex 静态四计数（indexCalls/locateCalls/skippedInvalid/chunksIndexed）+ 嵌套 ChunkIndexOpStats + stats()/resetForTest() + 有效/无效/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（index 入口与切片循环/locate 入口/静默跳过点）
- [x] ChunkIndexOpStats 嵌套 record + stats() + resetForTest()
- [x] ChunkIndexOpStatsTest（有效/无效/守恒/归零四测）
- [x] spec 1115 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='ChunkIndexOpStatsTest'` 全绿 + 既有 SemanticChunkIndex 回归绿。commit 见本轮 `feat(spill)` 提交。
