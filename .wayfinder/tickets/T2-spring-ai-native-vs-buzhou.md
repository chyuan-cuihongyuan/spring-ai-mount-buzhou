---
id: T2
title: Spring AI 2.0.0 原生能力 vs Buzhou 增强面（含 2.0.0 / Spring Boot 4.1.0 发布状态）
type: research
status: closed
assignee: wayfinder-chart-session
blocked-by: []
created: 2026-08-13
closed: 2026-08-13
---

## Question

1. 逐机制判定 Spring AI 2.0.0 原生已覆盖什么 / Buzhou 补什么 / 替代什么（`NATIVE`/`ADDS`/`REPLACES`），喂 [T3 验收基线](T3-depth-definition-of-done.md)。
2. 确认 `spring-ai 2.0.0` / `spring-boot 4.1.0` 真实发布状态，喂 [T1 CI 修复](T1-ci-red-remotely-green-locally.md)。

## Research findings（research subagent，2026-08-13，web 核验）

### 发布状态（直接喂 T1）
- **Spring AI 2.0.0 = GA**（2026-06-12 发布），**在 Maven Central**，基线 Spring Boot 4.0/4.1 + Spring Framework 7.0 + Jakarta EE 11。**无需 milestone/snapshot 仓。**
- **Spring Boot 4.1.0 = GA**（2026-06-10 发布），**在 Maven Central**，基线 Spring Framework 7.0.8+，JDK 17 起（JDK 21 在范围内）。
- → **结论**：Buzhou pom 无 `<repositories>` 是**正确**的；CI 失败不是「Central 缺制品」，是环境性（见 T1：疑似 GH Actions maven cache 的否定解析标记残留）。

### 逐机制对照（喂 T3 + item 6 边界文档）
| # | Buzhou 机制 | Spring AI 2.0 原生 | 关系 | 置信 |
|---|---|---|---|---|
| 1 | 渐进式记忆压缩 | 核心仅 `MessageWindowChatMemory`（按条数窗口）；**无 token 预算/摘要/压缩**。社区扩展 `spring-ai-session` 补事件源 ChatMemory + 可插拔压缩（含 LLM 摘要） | **ADDS** | 中高 |
| 2 | Spill 溢出 / read_range | **无**（无落盘、无大输出截断；最近的是 `ToolSearchToolCallingAdvisor` 减的是工具*schema* token，非输出） | **REPLACES** | 高 |
| 3 | Span+Event 认知可观测 | 成熟 Micrometer/OTel span+metrics（`gen_ai.*`）；但 **prompt/completion、工具入参/结果默认不采集，无 reasoning 捕获，无 span event**——是运维可观测，非认知 | **ADDS** | 高 |
| 4 | Skill 体系 | 无 prompt-skill；最近的是 `ToolSearchToolCallingAdvisor`（渐进披露**工具 schema**，非能力 prompt） | **REPLACES** | 中 |
| 5 | MCP 热插拔 | **原生**：MCP 动态工具增删、免重启、客户端感知、即时可用 | **NATIVE**（Buzhou 仅 wrapper） | 高 |
| 6 | 并行工具调用 | `DefaultToolCallingManager` GA **顺序执行**；`parallelToolCalls=true` 只控模型*返回*多调用，非客户端并行；**无 executor/超时/取消**；异步返回类型显式不支持。官方 workaround = 自定义 `ToolCallingManager` + 虚拟线程 | **REPLACES** | 高 |
| 7 | 原子工具 | `@Tool` 框架可暴露任意方法，但核心**无策展工具集**（无内置 file/command/http/task-list） | **ADDS** | 中高 |
| 8 | Hook 护栏 | Advisor 支持 pre/post 拦截、可阻断链（可做 HITL）、共享 context；内置 `SafeGuardAdvisor`（内容安全）。**无**打包 HITL advisor / 长产物护栏 / state-attachment 闭环 | **ADDS** | 高 |
| 9 | 持久化 SPI | `ChatMemoryRepository`（InMemory/JDBC/Cassandra/Neo4j/Mongo/Redis）+ `spring-ai-session` JDBC；**无** Summary/SessionState/SessionLease/Observability 专属 store 抽象 | **ADDS** | 中高 |

### 最大意外（须在深度基线里正面回应）
- **机制⑤ MCP 热插拔 = NATIVE**：Buzhou 在此只是 wrapper，非差异化。注：MCP 本就 out-of-scope（本次只做 core/memory/spill/guard），但要在边界文档里诚实标注。
- **机制⑥ 并行工具 = REPLACES（高置信）**：GA 顺序执行、无超时/取消——这是 Buzhou **最强存活差异化**，但护城河在实现深度而非概念（官方 workaround 同为虚拟线程自定义 Manager）。
- **机制③ 可观测**：应**整合 Micrometer/OTel** 而非另起管线；认知维度（证据/推理/span event）是真增量。
- **机制① 记忆**：应对照**社区 `spring-ai-session`**（而非裸核心）来定位，微压缩 + 九段摘要 + 动态预算仍超出它。

## Resolution

已由 research 子 agent 在 chart-the-map 会话解决。两条产出：
1. **发布状态**：`spring-ai 2.0.0` / `spring-boot 4.1.0` 均为 GA、在 Maven Central → [T1](T1-ci-red-remotely-green-locally.md) 根因从「缺仓」纠正为「环境性 maven 缓存」，修复方向锁定为清缓存/`-U`，**勿加 milestone 仓**。
2. **逐机制 NATIVE/ADDS/REPLACES 表**：成为 [T3 验收基线](T3-depth-definition-of-done.md) 的输入（避免与原生重复），并直接构成 item 6「Spring AI 边界文档」的事实骨架。MCP=NATIVE 须在边界文档诚实标注。

## Assets / Sources
- GA：https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/ · https://spring.io/blog/2026/06/10/spring-boot-4 · https://central.sonatype.com/artifact/org.springframework.ai/spring-ai-bom
- 原生能力：https://docs.spring.io/spring-ai/reference/api/chat-memory.html · /api/tools.html · /api/advisors.html · /observability/index.html · /api/tools/tool-search-tool.html
- 并行工具 issue：https://github.com/spring-projects/spring-ai/issues/4254 · /5195 · /4755
- MCP 动态工具：https://spring.io/blog/2025/05/04/spring-ai-dynamic-tool-updates-with-mcp
