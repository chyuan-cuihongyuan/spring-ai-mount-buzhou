# Wayfinder Map — Buzhou core 做深做透

## Destination

把 `buzhou-core` / `buzhou-memory` / `buzhou-spill` / `buzhou-guard` 四个核心机制做到**真实鲁棒**：先让 CI 在干净 runner 上真正绿（根因 T1 已定、执行 T10），摸清 Spring AI 2.0 原生边界（T2），再锚定「做深做透」的量化验收基线（T3），随后按基线逐机制深化。用户列出的 1-7 信誉项（措辞降级 / 可运行 demo / 真实 LLM 集成测试 / Spring AI 边界文档 / run_command 安全默认 / `.scratch` 卫生）作为**深化过程的副产品**收口，不另起 effort。目的地是「core 先做深」，不是「打满九机制」。

## Notes

- **领域**：Spring AI 2.0.0 之上的 Agent 运行时 Harness（JDK 21 / Spring Boot 4.1 / 虚拟线程）。术语见仓库 `CONTEXT.md`，机制详设见 `docs/spec/`。
- **KB 门禁**：下钻业务源码前先按仓库根 `AGENTS.md` 的 KB 路由（`.Knowledge/manifest-routing.json`）。
- **每会话**：先读本 MAP → 从 frontier 取一张 ticket → resolve 后回写「Decisions so far」。
- **建造 Spec（ready-for-agent）**：[docs/spec/11-best-of-breed-adoption.md](../../docs/spec/11-best-of-breed-adoption.md) —— Tier-1 跨 core/memory/spill/guard 的 PRD（问题/方案/用户故事/实现·测试决策/接缝/Out-of-Scope）；执行切片 = frontier **T12–T15**（每张含 Tier-1/2/3 清单，源于 [T11](../tickets/T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md`）。
- **tracker 约定**：见 `.wayfinder/README.md`。
- **已知事实（2026-08-13 核验）**：
  - CI badge = `failing`，但本地 `mvn -B -ntp clean verify`（与 `ci.yml` 同命令）**15 模块全绿**。
  - 本地非 clean 的 `mvn verify` 曾报 15 个 `NoSuchMethodError`——那是 `target/test-classes` 里**已删除且从未提交**的旧测试（`*DedupTest` / `CrashRecovery*`）残留 `.class` 造成的幽灵错误，非真实缺陷，`mvn clean` 即消。
  - **CI 根因已被 T1 research 二次纠正**：`spring-ai 2.0.0` / `spring-boot 4.1.0` **均为 GA、在 Maven Central**，pom 无 `<repositories>` 是对的；本地能解析全部依赖（取自 Central）。先前「`.lastUpdated` 缓存否定标记」假设**已被推翻**（24h 重查会自愈、doc-only 提交同样失败、`setup-java` 缓存步骤成功）——真实根因是**确定性 Linux/JDK21 构建测试缺陷**（显眼嫌疑 `/bin/sh`/CRLF/JDK8 均已排除为 Windows 本地假红、blob 全 LF；Linux 特有失败身份未知、需日志）。具体哪条失败需 CI 日志 / Linux 复现，见 [T10](../tickets/T10-fix-ci-os-specific-defect.md)（HITL/环境）。正确配置下本地绿可信，core 深化（T3+）不必等 badge 绿。
  - GitHub Actions 取证：仓库已开源，公开 API 免鉴权可读 run/step 结论与 annotation；**完整日志文本**需鉴权（`gh` 未登录、job-logs API 403），Linux 复现亦不可得（本机 WSL 无发行版、无 Docker）——故 T1 已取到「哪步失败」但未取到「Maven 报错原文」，执行尾交 [T10](../tickets/T10-fix-ci-os-specific-defect.md)。

## Decisions so far

- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面](../tickets/T2-spring-ai-native-vs-buzhou.md) — `spring-ai 2.0.0` / `spring-boot 4.1.0` **均为 GA、在 Maven Central**（→ [T1](../tickets/T1-ci-red-remotely-green-locally.md) 进一步**推翻缓存假设**、定为 OS 缺陷）；逐机制 NATIVE/ADDS/REPLACES 表成为 [T3](../tickets/T3-depth-definition-of-done.md) 与边界文档 [T9](../tickets/T9-spring-ai-boundary-doc.md) 的事实骨架；**MCP 热插拔=NATIVE** 须诚实标注。
- [.scratch/ 移出 git 跟踪 + 加入 .gitignore](../tickets/T7-remove-scratch-from-git.md) — 59 个内部草稿 `.md` 经六类敏感扫描**无任何命中** → **仅 untrack**（`git rm -r --cached` + `.gitignore`），保留历史、不做 `filter-repo` 重写；`CLAUDE.md`/`docs/agents/issue-tracker.md` 的引用为路径模板、移出后仍成立；实现见 [impl/02](../impl/02-untrack-scratch.md)。
- [README "生产就绪"措辞降级](../tickets/T8-downgrade-production-wording.md) — README 4 处「生产」措辞（L3 英 / L5 中 intro、L17 为什么需要、L25 能力段）降级为「**面向生产场景设计的实验性框架**」、中英同步、L25 显式锚定「项目状态：alpha」；`CONTEXT.md`/`docs/spec/00-overview.md` 的「跑在生产里」为术语定义/设计意图、无 alpha 矛盾、有意不动；实现见 [impl/03](../impl/03-readme-wording-downgrade.md)。
- [撰写「与 Spring AI 原生能力边界」文档](../tickets/T9-spring-ai-boundary-doc.md) — `docs/spec/10-spring-ai-boundary.md`（中英双语）落位 docs/spec 第 10 篇、README 加链接；T2 九机制全覆 + 置信标注，REPLACES（Spill/并行工具/Skill）/ ADDS（记忆/可观测/Hook/持久化/原子工具）/ **NATIVE（MCP 热插拔 诚实标注非差异化）**；实现见 [impl/04](../impl/04-spring-ai-boundary-doc.md)。
- [run_command 默认关闭 vs 沙箱](../tickets/T6-run-command-safety-default.md) — **默认关**（`ToolsModule.Builder` 既有默认，已由 `ToolsModuleTest` 守护）；沙箱方案=否（黑名单+FileSandbox+超时+HITL 多层已足、跨平台沙箱成本不值）；opt-in 经 `enabledDangerousToolNames()` 挂 HITL；`/bin/sh` POSIX 约束写入 `RunCommandTool` javadoc；implementer 确认既有安全默认（用户未应答 grilling、可推翻）；实现见 [impl/07](../impl/07-run-command-safe-default.md)。
- [CI 在 GitHub 红而本地绿的根因](../tickets/T1-ci-red-remotely-green-locally.md) — 公开 API 取证：最近 **8 连红**（含 doc-only 提交）、恒挂在 ci.yml `Build & test`（`mvn -B verify`）exit 1、`setup-java` 缓存步骤成功；**推翻「`.lastUpdated` 缓存」假设**（24h 自愈 + T2 证依赖 GA 在 Central + 本地解析成功），定为**确定性 Linux/JDK21 构建测试缺陷**（`/bin/sh`/CRLF/JDK8 均已排除为 Windows 本地假红、blob 全 LF；Linux 特有失败身份未知）；具体失败行需日志 / Linux 复现 → graduate [T10](../tickets/T10-fix-ci-os-specific-defect.md) 执行；正确配置下本地绿可信、T3+ 不必等 badge。
- [可运行 src/main demo 的形态](../tickets/T4-runnable-main-demo.md) — `examples/src/main/.../BuzhouDemo`（纯编程式 `Buzhou.runtime` + `MemoryModule`，无 key 即跑）；**stub-first + 可插真 key**（`run(ChatModel)`，`main` 默认 `StubChatModel`）；预置 10 轮排障历史触发微压缩 + `read_evidence` 回查；`BuzhouDemoTest` 守回归、`main()` 实跑输出可见；README「方式三」snippet 同 API 互证；examples/pom 加 compile-scope core+memory；实现见 [impl/05](../impl/05-runnable-main-demo.md)。
- [真实 LLM 集成测试策略](../tickets/T5-real-llm-integration-test.md) — **双轨**（同一条 core 链：多轮+工具+压缩）：Mock 变体（反应式模型按输入决策工具调用）进默认 `mvn verify`、CI 绿；真实 API 变体 `@EnabledIfEnvironmentVariable("BUZHOU_LLM_API_KEY")` + `@SpringBootTest`（`spring-ai-starter-model-openai` 自动装配 ChatModel）凭据门控、CI 跳过、弱断言防脆；实现见 [impl/06](../impl/06-real-llm-integration-test.md)。
- [core/memory/spill/guard 做深做透 DoD 基线](../tickets/T3-depth-definition-of-done.md) — ratify SPEC 既列判据为基线 + **审计既有测试为证据**：四模块深度判据已被既有（Linux-green）套件满足（memory 微压缩占位符/P0-P3 优先级、spill read_range 多模式+读写失败语义非对称、guard HITL→state→Attachment 跨三模块端到端、core 并行/超时/崩溃续跑/去重）；SPI 冻结由 `AbstractBuzhouStoresContractTest` × 4 后端证明——无需新增冗余测试；实现见 [impl/08](../impl/08-depth-tests-four-mechanisms.md)。
- [各机制「对标开源最优」best-of-breed 萃取](../tickets/T11-oss-best-ideas-core-memory-spill-guard.md) — 4 并行子 agent 横扫 LangGraph/MemGPT/LangChain/Mem0/Zep/Claude Code/Codex/Aider/NeMo/Guardrails/Cedar/MSRC… 产出 `docs/research/oss-best-of-breed.md`：每模块 Tier1/2/3 采纳 backlog + 「Buzhou 已领先」清单。**注**：T3 闭合于「SPEC 判据已满足」；本票把杆抬到用户诉求的「对标开源最优」——两者非矛盾、是不同刻度（SPEC 达标 vs 业界最优）。
- [docs/spec/11 Tier-1 落地（T16–T27 全闭合）](../tickets/T12-core-best-of-breed.md) — core/memory/spill/guard 四模块各引入一项业界 best-of-breed 思想并强化既有真原创：**core** 错误回喂（T16）+ 有界 Turn（T17）；**guard** 读侧 spotlighting+canary（T18）+ on_fail 动词汇（T19）；**spill** 自描述 Handle/token 阈值（T20）+ hot-tail 两级保留（T21）+ durable override（T22）；**memory** 预算渲染（T23）+ 增量摘要（T24）+ Mem0 对账（T25）+ 双时序（T26）+ compact_now（T27）。spec 01/02/05/07 同步修订；双轴 code-review（Standards+Spec）修复 3 个交互缺陷（T18×T20 包裹破坏形状识别、T26 新版本未持久化、T27 绕过对账）；本地 `mvn -B -ntp clean verify` 16 模块全绿。epic：[T12](../tickets/T12-core-best-of-breed.md) / [T13](../tickets/T13-memory-best-of-breed.md) / [T14](../tickets/T14-spill-best-of-breed.md) / [T15](../tickets/T15-guard-best-of-breed.md) Tier-1 部分随本行闭合，Tier-2/3 留后续。

## Not yet specified

- ~~core / memory / spill / guard 的「对标开源最优」深度 ticket~~ → **已 graduate 为 [T12 core](../tickets/T12-core-best-of-breed.md) / [T13 memory](../tickets/T13-memory-best-of-breed.md) / [T14 spill](../tickets/T14-spill-best-of-breed.md) / [T15 guard](../tickets/T15-guard-best-of-breed.md)**（每张含 Tier1/2/3 落地清单，源于 [T11](../tickets/T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md`）。T3 闭合于「SPEC 判据满足」；T12–T15 把杆抬到用户诉求的「对标开源最优」——不同刻度、非矛盾。
- ~~item 6「Spring AI 边界文档」~~ → 已 graduate 为 [T9](../tickets/T9-spring-ai-boundary-doc.md)（基于 T2 表）。
- T2 已揭示 MCP 热插拔 = NATIVE；该结论已注入 T9，MCP 本身维持 out-of-scope，无需再范围 ticket。
- 跨 OS 测试健壮性（`/bin/sh` 硬编码、路径大小写、charset/locale 依赖）——[T10](../tickets/T10-fix-ci-os-specific-defect.md) 先修已知缺陷；「做深」是否把「干净 Linux/CI 上稳定绿」纳入每模块 DoD，等 [T3 验收基线](../tickets/T3-depth-definition-of-done.md) 定。
- crash-recovery / 并行工具调用 / 动态预算 的正确性深挖范围，部分依赖 T1 结果与 T3 基线。

## Out of scope

- `buzhou-observe-otel` / `buzhou-observe-dashboard` / `buzhou-mcp` / `buzhou-skills` / `buzhou-tools`（除 run_command 安全）的「做深」——本次目的地只含 core+memory+spill+guard，其余维持现状。
- 发布到 Maven Central（已有 `RELEASING.md` 流程，属后续独立 effort）。
- 除「一个可运行 src/main demo + 一个真实行为集成测试」外的 `examples/` 扩展。
- 从 git 历史抹除 `.scratch`（除非 T7 发现含敏感信息；默认仅 untrack）。

## Tickets

开放 ticket 在 `tickets/`；frontier = `status:open` + `assignee:""` + 无未闭合 `blocked-by`。索引（含依赖）：

- [CI 在 GitHub 红而本地绿的根因与修复](../tickets/T1-ci-red-remotely-green-locally.md) — `research` · ✅ **closed**（根因=确定性 OS 缺陷，推翻缓存假设；执行尾见 T10）
- [Spring AI 2.0.0 原生能力 vs Buzhou 增强面（含 2.0.0/4.1.0 发布状态）](../tickets/T2-spring-ai-native-vs-buzhou.md) — `research` · ✅ **closed**
- [core/memory/spill/guard "做深做透"的验收基线](../tickets/T3-depth-definition-of-done.md) — `grilling` · ✅ **closed**（ratify SPEC 判据 + 审计既有测试为证据；T2 已闭合，解锁）
- [可运行 src/main demo 的形态](../tickets/T4-runnable-main-demo.md) — `prototype` · ✅ **closed**（stub-first + 可插真 key；BuzhouDemo 入口；CI 绿由 T10 单独追踪、不阻塞形态决策）
- [真实 LLM 集成测试策略](../tickets/T5-real-llm-integration-test.md) — `prototype` · ✅ **closed**（Mock 进 CI + 真实 gated；CI 绿由 T10 单独追踪、不阻塞策略决策）
- [run_command 默认关闭 vs 沙箱执行](../tickets/T6-run-command-safety-default.md) — `grilling` · ✅ **closed**
- [.scratch 移出 git 历史 + 加 .gitignore](../tickets/T7-remove-scratch-from-git.md) — `task` · ✅ **closed**
- [README "生产就绪"措辞降级，正文与 alpha 对齐](../tickets/T8-downgrade-production-wording.md) — `task` · ✅ **closed**
- [撰写「与 Spring AI 原生能力边界」文档（item 6）](../tickets/T9-spring-ai-boundary-doc.md) — `task` · ✅ **closed**（T2 已闭合，解锁）
- [取 CI 失败日志/ Linux 复现 → 修 OS 缺陷 → badge 转绿](../tickets/T10-fix-ci-os-specific-defect.md) — `task` · **frontier**（HITL/环境）
- [各机制「对标开源最优」best-of-breed 技术萃取](../tickets/T11-oss-best-ideas-core-memory-spill-guard.md) — `research` · ✅ **closed**（产出 `docs/research/oss-best-of-breed.md`）
- [core 对标开源最优——epic](../tickets/T12-core-best-of-breed.md) — `task` · **epic**（Tier1/2/3 清单；细化见 T16–T17）
- [memory 对标开源最优——epic](../tickets/T13-memory-best-of-breed.md) — `task` · **epic**（细化见 T23–T27）
- [spill 对标开源最优——epic](../tickets/T14-spill-best-of-breed.md) — `task` · **epic**（细化见 T20–T22）
- [guard 对标开源最优——epic](../tickets/T15-guard-best-of-breed.md) — `task` · **epic**（细化见 T18–T19）
- [core · 工具错误回喂模型](../tickets/T16-core-tool-error-feedback.md) — `task` · **frontier** · epic T12
- [core · 有界 Turn + 可组合停止条件](../tickets/T17-core-bounded-turn.md) — `task` · **frontier** · epic T12
- [guard · 读侧注入防御 spotlighting + canary](../tickets/T18-guard-read-side-injection-defense.md) — `task` · **frontier** · epic T15
- [guard · 读写失败 on_fail 动词汇](../tickets/T19-guard-on-fail-vocabulary.md) — `task` · **frontier** · epic T15
- [spill · 自描述 Handle + token-aware 阈值](../tickets/T20-spill-self-describing-handle.md) — `task` · **frontier** · epic T14
- [spill · hot-tail/cold-storage 两级保留](../tickets/T21-spill-hot-tail-cold-storage.md) — `task` · blocked-by T20 · epic T14
- [spill · per-tool durable override](../tickets/T22-spill-per-tool-durable-override.md) — `task` · blocked-by T20 · epic T14
- [memory · 动态预算渲染给模型](../tickets/T23-memory-budget-render-to-model.md) — `task` · **frontier** · epic T13
- [memory · 增量摘要 summarized_message_ids](../tickets/T24-memory-incremental-summary.md) — `task` · **frontier** · epic T13
- [memory · Mem0 事实对账](../tickets/T25-memory-mem0-fact-reconciliation.md) — `task` · blocked-by T24 · epic T13
- [memory · 双时序事实有效性](../tickets/T26-memory-bi-temporal-fact-validity.md) — `task` · blocked-by T25 · epic T13
- [memory · 语义边界压缩触发 compact_now](../tickets/T27-memory-semantic-boundary-compact-trigger.md) — `task` · **frontier** · epic T13

- [core · 工具错误回喂模型](../tickets/T16-core-tool-error-feedback.md) — `task` · ✅ **closed**（ToolErrorFeedback 统一「错误即反馈」通道；Turn 不死、每 tool_call 恒一响应；spec05 同步）
- [core · 有界 Turn + 可组合停止条件](../tickets/T17-core-bounded-turn.md) — `task` · ✅ **closed**（TurnLoopPolicy + BoundedToolCallingAdvisor；默认 40 轮上界、Predicate and/or 组合、优雅收尾 + turn.loop.bounded 事件）
- [guard · 读侧注入防御 spotlighting + canary](../tickets/T18-guard-read-side-injection-defense.md) — `task` · ✅ **closed**（SpotlightHook/CanaryGuardHook + Spotlighting 共享格式；canary 前置注入、变体 n-gram 自硬化；默认关、injectionDefense() 一键开）
- [guard · 读写失败 on_fail 动词汇](../tickets/T19-guard-on-fail-vocabulary.md) — `task` · ✅ **closed**（OnFail 枚举套既有非对称不改语义；REFRAIN 可配、事件带 onFail；REASK=T16 通道 + T17 上界）
- [spill · 自描述 Handle + token-aware 阈值](../tickets/T20-spill-self-describing-handle.md) — `task` · ✅ **closed**（占位符含形状/大小/回读动词 + 显式截断标记；thresholdTokens 全局/per-tool；重入幂等修复）
- [spill · hot-tail/cold-storage 两级保留](../tickets/T21-spill-hot-tail-cold-storage.md) — `task` · ✅ **closed**（HotTailViewProcessor 视图级溢出；merge 多 viewProcessor 链式组合；与即时 offload 互斥强制）
- [spill · per-tool durable override](../tickets/T22-spill-per-tool-durable-override.md) — `task` · ✅ **closed**（spillNeverOffload 双路径生效；策略判定收敛 SpillThresholds 单一事实源）
- [memory · 动态预算渲染给模型](../tickets/T23-memory-budget-render-to-model.md) — `task` · ✅ **closed**（SegmentBudgetPlanner：P0-P3 按占比拆字符预算、每段 chars_current/limit 页脚 + 头部提示）
- [memory · 增量摘要 summarized_message_ids](../tickets/T24-memory-incremental-summary.md) — `task` · ✅ **closed**（消息级水位 × 轮次水位双保险；只折新消息、代际连续；__summarizedMessageIds 持久化）
- [memory · Mem0 事实对账](../tickets/T25-memory-mem0-fact-reconciliation.md) — `task` · ✅ **closed**（SummaryFactReconciler 四态裁决 + 对账正文应用；NOOP 韧性；memory.fact.reconciled 可观测；默认开）
- [memory · 双时序事实有效性](../tickets/T26-memory-bi-temporal-fact-validity.md) — `task` · ✅ **closed**（BiTemporalFactLedger 标失效不删除；valid_from/until、historyOf/validAt 时序回查）
- [memory · 语义边界压缩触发 compact_now](../tickets/T27-memory-semantic-boundary-compact-trigger.md) — `task` · ✅ **closed**（CompactNowTool 双触发路径；幂等双水位；不绕过对账；有摘要模型默认注册）

**Frontier（本会话后可领取）**：T10（HITL/环境）。（T1–T9、T11 已闭合；T12–T15 升为 epic、由 T16–T27 细化并**全部闭合**——docs/spec/11 Tier-1 跨四模块落地完成，本地 `mvn -B -ntp clean verify` 16 模块全绿；Tier-2/3 仍留 spec 11 Out of Scope。）
