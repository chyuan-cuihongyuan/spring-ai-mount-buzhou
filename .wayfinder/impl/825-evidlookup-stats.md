# 825 — evidence_lookup 证据回查读面

**What to build:** EvidenceLookupTool 静态五计数（calls/misses/hits/completeReads/slicedReads）双守恒 + 嵌套 EvidenceLookupStats + stats()/resetForTest() + 命中/切片/未找到/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（入口/命中/未找到/全文/切片）
- [x] EvidenceLookupStats 嵌套 record + stats() + resetForTest()
- [x] EvidenceLookupStatsTest（全文/切片/未找到/双守恒/reset 五测）
- [x] spec 1073 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='EvidenceLookupStatsTest'` 全绿 + 既有回查回归绿。commit 见本轮 `feat(memory)` 提交。
