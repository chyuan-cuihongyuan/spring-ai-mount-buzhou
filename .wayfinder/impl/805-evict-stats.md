# 805 — evict_handle 逐出判定读面

**What to build:** EvictHandleTool 静态四计数（attempts/evictions/badPathRejects/parseRejects）+ 嵌套 EvictStats + stats()/resetForTest() + 逐出/坏路径/坏 JSON/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 四计数落点（入口/成功 markEvicted/URI 前缀拒/catch 兜底）
- [x] EvictStats 嵌套 record + stats() + resetForTest()
- [x] EvictStatsTest（合法逐出/坏路径/坏 JSON/守恒/reset 五测）
- [x] spec 1053 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='EvictStatsTest'` 全绿 + 既有 EvictHandleTool 回归绿。commit 见本轮 `feat(spill)` 提交。
