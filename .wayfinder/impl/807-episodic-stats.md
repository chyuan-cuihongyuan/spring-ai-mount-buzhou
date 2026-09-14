# 807 — 情景记忆读写双守恒读面

**What to build:** EpisodeLedger 静态九计数双守恒（写侧 recordCalls/recorded/recordDropped/recordFailures + 读侧 recallCalls/recallHits/recallEmpties/recallDropped）+ 嵌套 EpisodicMemoryStats + stats()/resetForTest() + 双守恒测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [ ] 九计数落点（record 三分支 + recall 四分支；fewShotBlock 复用 recall 计数）
- [ ] EpisodicMemoryStats 嵌套 record + stats() + resetForTest()
- [ ] EpisodicMemoryStatsTest（写入/丢弃/失败/命中/空召/双守恒/reset 七测）
- [ ] spec 1055 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory -am test -Dtest='EpisodicMemoryStatsTest'` 全绿 + 既有 episodic 回归绿。commit 见本轮 `feat(memory)` 提交。
