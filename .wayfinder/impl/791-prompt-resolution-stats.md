# 791 — 提示词注册表解析分布读面

**What to build:** InMemoryPromptRegistry resolutions/hits/misses 三计数（公共解析核心不重复计）+ 嵌套 PromptResolutionStats + resolutionStats() + 解析分布测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数 + 公共解析核心收敛（不重复计）
- [x] PromptResolutionStats 嵌套 record + resolutionStats()
- [x] PromptResolutionStatsTest（LATEST/label 命中未命中/守恒/fresh 零值）
- [x] spec 1039 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='PromptResolutionStatsTest'` 全绿。commit 见本轮 `feat(core)` 提交。
