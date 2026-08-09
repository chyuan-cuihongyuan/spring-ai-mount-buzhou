# 21 — examples 排障 demo 与评测

**What to build:** 运维排障 Agent 单场景多脚本：记忆压缩链/可观测回放/护栏与 HITL/Skill+MCP 热插拔四簇脚本，mock DB+HTTP 设施；评测脚本独立目录（复用 mock）：四指标（P0 保留率/续接成功率/事实召回/压缩率）+20 轮端到端两级联动用例，接入 CI 作回归门禁；README quickstart 指向 demo。

**Blocked by:** 13, 14, 15, 16, 17

**Status:** done

- [x] 排障 demo 一条命令跑通四簇脚本（README 可复现）
- [x] 评测四指标出报告，阈值回归挂 CI
- [x] demo 覆盖：Spill 触发与回读、HITL 确认往返、Skill 加载、MCP 热更、压缩后 P0 锚定
- [x] 评测脚本与 demo 分目录、共享 mock 设施

## 收口（done）

- 四簇 demo（`examples/src/test/.../demo/`）：`MemoryCompactionDemoTest`（微压缩占位+evidence / 九段摘要 P0 锚定 / Spill 落盘+read_range 回读）、`ObservabilityReplayDemoTest`（Span 树层级 / 注入快照 / 思维链）、`GuardAndHitlDemoTest`（HITL 阻断→事件→授权→放行 / 授权跨实例持久）、`SkillAndMcpDemoTest`（Skill catalog+load_skill / MCP 差量热更）。
- 评测（`examples/src/test/.../evaluation/SummaryEvaluationTest`）：四指标达标——P0 保留率 100%、关键事实召回 100%、token 压缩率 9%、续接目标保留通过；阈值断言随 `mvn verify` 作 CI 门禁；LLM-as-judge 方法论写 javadoc/README（CI 不强制）。
- 共享夹具 `support/TroubleshootingFixture`；2 个既有测试（AtomicTools/SkillIntegrationTest）改复用 `buzhou-core` test-jar 的 `ScriptedChatModel`（消除两处内嵌重复）；evidence 实际回查（read_evidence 取回原文）+ 两级先后次序（微压缩不产摘要）断言已补。
- pom 补 `buzhou-mcp` / `buzhou-observability` test 依赖；新增 `examples/README.md`。
- examples 全量 19 测试通过。
