# 903 — 全模块测试补全覆盖 R1

**What to build:** 6 模块 20 个 JaCoCo 零覆盖靶点的直测套件（core 14 文件 + guard/spill/resilience/mcp 各 1 文件），行为语义断言为主，零覆盖清零 + 豁免入档。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] core：Spotlighting / TableContextWindowResolver / ConfigMaps / SessionInterrupts / ToolSetSpec / CompositeAttachmentRenderer / RecoverySupport / EventType / RunStateTrackerHook / EmbeddingProvider / CompositeBuzhouMetrics / SessionStateHandle / OnFail / SessionStateStore default 体
- [x] guard：BuzhouGuardHealthAutoConfigurationTest（三态 + indicator 透传）
- [x] spill：BuzhouSpillHealthAutoConfigurationTest（UP/UNKNOWN/DOWN）
- [x] resilience：BuzhouResilienceHealthAutoConfigurationTest（直接 new + runner 装配面）
- [x] mcp：JdbcToolSetSpecStoreTest（H2 内存库）+ pom h2 test 依赖
- [x] 测试显形缺陷独立修复：T1803（guard indicator 条件装配）/ T1804（mcp CLOB→TEXT + PG 回归）/ T1805（ToolDenialLog 有序快照）
- [x] spec 1200 + README 行

## Done

验证：各模块定向测试绿；core 零覆盖复扫清零（豁免 2 项与台账一致）。commit 见本轮 `test(core,guard,spill,resilience,mcp)` 提交。
