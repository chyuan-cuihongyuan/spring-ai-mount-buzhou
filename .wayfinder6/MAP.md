# Wayfinder Map — Buzhou 生产级纵深（effort #6）

> effort #6，延续 [`.wayfinder/`](../.wayfinder/MAP.md)（#1）、[`.wayfinder2/`](../.wayfinder2/MAP.md)（#2）、[`.wayfinder3/`](../.wayfinder3/MAP.md)（#3）、[`.wayfinder4/`](../.wayfinder4/MAP.md)（#4）、[`.wayfinder5/`](../.wayfinder5/MAP.md)（#5：韧性/成本/工具面/会话/基建 22 轮，T81–T102，impl 56–77）。

## Destination

把 effort #5 收口后仍开放的生产级缺口全部闭合，并把各模块继续做深做透至「真正可生产」：
投递可靠性（webhook 持久化 outbox）、韧性自适应（熔断半开参数）、数据生命周期（fork evidence 归属）、
输入面（多模态透传）、数据可移植（会话导出/导入）、运维面（store fsck、会话枚举索引）、
上下文防护（MCP 大结果）、质量面（黄金轨迹回归评估）。到达 = 20+ 轮「wayfinder→to-spec→to-tickets→implement」
自迭代落地、全仓 verify 绿、硬门与文档齐备、MAP 闭合。

## Notes

- **领域**：Spring AI 2.0.0 之上的单 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见 `CONTEXT.md`，机制详设见 `docs/spec/`（00–23 已有，本 effort 增 24 号起）。
- **用户常设授权（2026-08-15，effort #6 延续）**：全程「不需询问、全部自决、按推荐迭代」——决策票以 AFK 方式闭合（Resolution 注明可推翻）；to-spec 的 seam 确认与 to-tickets 的 breakdown quiz 同样按推荐自决；20–25 轮完整流程自迭代。
- **10K★ 政策**：借鉴对象只认 ≥10K★ OSS（LangChain/LangGraph ~100K★、OpenHands ~50K★、AutoGen ~40K★、Dify ~100K★、CrewAI ~30K★、aider ~30K★、Spring AI 生态本身等）；语义借鉴优先、不轻易引新依赖；不达标依赖不得进 runtime classpath。事实源：2026-08-15 本地全仓勘察（effort #6 图前广度扫描：HITL 守卫/429 Retry-After/token 预估/SchemaMigrator/优雅停机/指标基数纪律 均已存在；缺口清单见首张票）+ 既往 `docs/research/` 五份。
- **测试哲学不变**：好测试只测外部行为；主接缝 = examples 端到端（FakeChatModel/ScriptedChatModel 驱动）；store 契约测试沿用 `AbstractBuzhouStoresContractTest`；测试不得 import 他模块 `internal` 包。
- **每轮流程**：解 1 张决策票 → /to-spec 增量（新 24 号起 spec）→ /to-tickets 切片 → /implement → 模块级测试 → emoji 规范 commit；里程碑轮全仓 `mvn clean verify`。
- **tracker 约定**：见 [README.md](README.md)；票号 T103 起全局续用；impl 切片 78 起续用。

## Decisions so far

- [T103 webhook 持久化 outbox](tickets/T103-webhook-durable-outbox.md) — SessionStateStore 合成会话键空间（`__buzhou.webhook__`）承载 outbox/dead；emit 同步入队、到期轮询投递、成功即删、记录级持久退避、max-attempts（默认 8）死信隔离可查询；at-least-once + 幂等键契约不变；spec 24。
- [T104 熔断冷却自适应退避](tickets/T104-adaptive-half-open.md) — 连续跳闸驱动冷却指数退避（×2^(trips-1) 封顶 backoff-cap 默认 8），探测成功即复位；生效冷却贯穿 admit/占位/逃生；事件 payload + gauge + stats 快照；首跳行为零变化；spec 25。
- [T105 fork 证据归属与生命周期](tickets/T105-fork-evidence-lifecycle.md) — 引用计数共享：fork 登记引用（持久账本 .evidence-refs.json）、源删除被引用证据保留、最后引用者关闭物理删、TTL/孤儿扫描门控、悬垂读 EVIDENCE_GONE；core forkListeners 第 11 槽；spec 26。
- [T106 多模态输入透传](tickets/T106-multimodal-input.md) — MediaRef(mimeType, URI) 三入口（chat/stream/chatForEntity，default UOE）；mediaRefs 落 metadata 持久化；重发只随最新带媒体消息（旧轮降级标记）；token 每媒体固定 320；spec 27。
- [T107 会话可移植导出/导入](tickets/T107-session-export-import.md) — SessionExport 单 JSON 文档（messages+summary+state，epoch-millis DTO）；导入默认 Id 重映射/keepIds 冲突 fail-fast；spill 引用清单派生；导入会话可 spawn 续用；spec 28。
- [T108 store fsck 一致性校验](tickets/T108-store-fsck.md) — StoreFsck.run 只读对账（四检测项：孤儿摘要/残留 state/泄漏租约/悬挂观测，全集=观测留痕+extras）+ repair 按项可选清除（观测永不自动清）；spec 29 + runbook 引用。
- [T109 会话索引与枚举](tickets/T109-session-index.md) — SessionIndexStore SPI（upsert/get/list/delete）+ SessionIndexObserver 生命周期维护（最终一致）+ 内存/JDBC V3/Redis 三实现与自动装配；未装配零影响；spec 30。
- [T110 工具结果尺寸防护](tickets/T110-mcp-result-guard.md) — ToolResultLimiter（默认 20K 字符截断+提示尾，glob per-tool 豁免，read_range 默认豁免）；HarnessToolCallingManager 统一出口 + Holder 全局默认 + buzhou.tools.* 配置；spec 31。
- [T111 黄金轨迹回归评估](tickets/T111-golden-trajectory-eval.md) — EventSequenceAssert（子序列/间隔/计数/payload，attach+attachGlobal）+ 六条黄金轨迹（降级链/预算/配额/熔断恢复/REASK/fork）；机制语义两发现回写 spec；spec 32。

## Not yet specified

- **Skills 大目录评分/分页**：目录现线性全量注入系统提示词；技能数上百后注入体积与选择精度问题——等 T110（上下文防护）落地后评估是否复用同一截断/评分机制。
- **LLM 响应缓存**：LangChain set_llm_cache 语义；工具调用链下缓存正确性边界未想清（语义风险大）——等韧性自适应（T104）与预算（已有）视角合并后再评估。
- **dashboard 会话列表**：T109 会话枚举落地后，dashboard/ops 侧如何消费（前端工程化仍 out-of-scope，仅评估查询服务侧）。
- **发布流程 SBOM 附着**：T92 遗留 fog（SBOM 随 release 留档）；release.yml 已有，具体附着方式等运维面票（T108 fsck / T109）合并评估。

## Out of scope

- **多实例分布式接管、分布式限流/配额、分布式锁之外的分布式语义**（沿用 #2–#5 边界；单进程组件已在 runbook §6 显式文档化）。
- **Firecracker/E2E 沙箱完整档、FIDES 二期、sub-agent、跨 agent 共享记忆、多 agent 编排/workflow 引擎**（沿用 #2 边界；本仓定位单 Agent harness）。
- **LLM-as-judge 强制 CI 门禁**（保持可选方法论，不进硬门）。
- **dashboard 前端工程化、观测 OLAP、MCP server 侧实现**（沿用 #4 边界）。
- **音频/视频多模态**（若 T106 决定做图像透传，音视频仍 out；Spring AI Media 面向图片/PDF 场景）。

## Tickets

初始 frontier 9 张（均含 AFK 决议草案，按轮逐张闭合）：

- [T103 webhook 持久化 outbox](tickets/T103-webhook-durable-outbox.md)
- [T104 熔断半开自适应](tickets/T104-adaptive-half-open.md)
- [T105 fork evidence 归属与生命周期](tickets/T105-fork-evidence-lifecycle.md)
- [T106 多模态输入透传](tickets/T106-multimodal-input.md)
- [T107 会话导出/导入](tickets/T107-session-export-import.md)（blocked-by T105、T106）
- [T108 store fsck 一致性校验](tickets/T108-store-fsck.md)（blocked-by T105）
- [T109 会话枚举与元数据索引](tickets/T109-session-index.md)
- [T110 工具结果尺寸防护](tickets/T110-mcp-result-guard.md)
- [T111 黄金轨迹回归评估](tickets/T111-golden-trajectory-eval.md)（blocked-by T104）

随决策推进从 fog 毕业新票。
