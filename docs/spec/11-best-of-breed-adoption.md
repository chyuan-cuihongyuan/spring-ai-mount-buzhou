# 11 对标开源最优：core / memory / spill / guard Tier-1 落地 Spec

> 本 Spec 由 wayfinder 研究票 [T11](../../.wayfinder/tickets/T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md` 综合而成，把用户「对标开源最优、选最优思想写入」的诉求转化为可建造的 Tier-1 增量。遵守仓库铁律 **「改机制先改 Spec」**：本 Spec 落地时须同步修订 [01 记忆压缩](01-memory-compaction.md) / [02 Spill](02-spill.md) / [05 并行工具](05-parallel-tools.md) / [07 Hook 护栏](07-hooks.md) 的对应章节。领域术语以根目录 `CONTEXT.md` 为准。

## Problem Statement

Buzhou 的 core / memory / spill / guard 四机制目前满足自定 SPEC 判据（详见 wayfinder T3 决议），但**未到「对标开源最优」**。横扫 LangGraph / MemGPT(Letta) / LangChain / Mem0 / Zep / Claude Code / Codex / Aider / NeMo / Guardrails / Cedar / MSRC / Rebuff 后，暴露出四类用户可感知的差距：

- **core**：单个工具抛异常会**终结整轮 Turn**（而非让模型自我纠错继续）；think→tool 递归**无上界**，存在成本失控/死循环风险。
- **memory**：九段式摘要生成的事实**不去重、不对账**（重复/矛盾/陈旧累积）；动态预算算出来了但**不展示给模型**，模型无从主动节流；摘要每次可能**全量重算**（漂移累积）。
- **spill**：上下文里只剩**裸路径**占位符，模型常忘记回读或猜错数据形状；阈值若硬编码按字节/行计，会**静默截断**（业界已多次因此让 agent 编造中段）。
- **guard**：读侧把工具/RAG 输出回灌 prompt 时**无注入隔离**（间接 prompt 注入是已建档的最高风险面）；读写失败语义**缺统一动词**，难与业界护栏词汇对齐。

## Solution

按四模块各引入一项业界 best-of-breed 思想（Tier-1，廉价高 ROI），**保住并放大 Buzhou 既有的真原创**（并行工具+虚拟线程、微压缩 evidence-id 确定性回读指针、九段 P0–P3 段内分级、动态预算、spill 三模回读、读写失败非对称、HITL session-state 授权、确定性事实采集 hook→state→Attachment），把四机制从「达标 SPEC」抬到「对标开源最优」。Tier-2/3（事件溯源、FIDES、Cedar、语义回读、AST 切片等）列为后续阶段，不在本 Spec 建造范围。

## User Stories

1. 作为 Buzhou 接入方，我希望**单个工具抛异常时 Turn 不死**，这样模型能拿到错误反馈自我纠错、继续完成用户请求，而不是整轮失败重试。
2. 作为 Buzhou 接入方，我希望**每轮 think→tool 递归有可配置上界**，这样即便模型陷入工具调用死循环，成本和延迟也有硬上限并优雅收尾。
3. 作为 Buzhou 接入方，我希望**停止条件可组合**（预算 / 超时 / 工具信号 / 外部取消），这样我能用声明式策略而非散落的 `if` 控制循环退出。
4. 作为 Agent 终端用户，我希望**模型偶发的工具误用能被自愈**，这样我少遇到「中途崩溃、请重试」的体验。
5. 作为 Buzhou 接入方，我希望**九段式摘要里的事实自动去重/对账**，这样长会话不会累积重复或自相矛盾的记忆。
6. 作为 Agent 终端用户，我希望**关键事实被取代时旧值不被断崖删除**，这样我能回溯「之前以为是什么」，且排障有据。
7. 作为 Buzhou 接入方，我希望**模型能看到自己的预算压力**（每段 chars_current/chars_limit），这样模型会主动削低优先级（P3）段、而非被动等到强制压缩。
8. 作为 Buzhou 接入方，我希望**模型能在任务边界主动触发压缩**（而非仅 token 兜底），这样压缩发生在干净边界、保真度更高。
9. 作为 Buzhou 接入方，我希望**摘要按增量折叠新消息**（只摘要未摘要部分），这样避免全量重摘要带来的漂移累积与重复成本。
10. 作为 Agent 终端用户，我希望**长会话压缩后关键信息仍在**，这样跨轮任务不会因为压缩断崖式丢上下文。
11. 作为 Buzhou 接入方，我希望**超大工具输出在上下文里是自描述占位符**（含句柄 + 数据形状 + 大小 + 精确回读动词），这样模型能可靠地按需回读、不再猜形状或忘记回读。
12. 作为 Buzhou 接入方，我希望**溢出阈值可配置且按 token 计**（非硬编码字节/行），并能按工具覆盖，这样不同工具的输出特性被尊重、避免静默截断。
13. 作为 Buzhou 接入方，我希望**近期工具结果全量内联、旧结果自动溢出**（hot-tail / cold-storage 两级），这样推理所需的近期数据零损失、旧数据不占窗口。
14. 作为 Buzhou 接入方，我希望**某些工具输出可声明「永不溢出」**（如 DB schema、整文件），这样对截断敏感的输出不被框架误处理。
15. 作为 Agent 终端用户，我希望**模型回读溢出数据时拿到的是真实切片**，这样基于回读的推理不会因数据残缺而编造。
16. 作为 Buzhou 接入方，我希望**读侧回灌 prompt 的工具/RAG 输出被标记为「仅数据」**（spotlighting），这样间接 prompt 注入无法伪装成指令。
17. 作为安全评审者，我希望**框架能检测注入是否真的影响了工具输出**（canary 泄漏），这样我有客观证据判断是否被投毒，并能自硬化拦截变体。
18. 作为 Buzhou 接入方，我希望**读写两侧的失败语义用统一动词汇表达**（读侧降级 / 写侧阻断 / 可恢复重试），这样护栏策略可声明式配置、与业界护栏（Guardrails `on_fail`）心智模型一致。
19. 作为 SRE/运维者，我希望**写侧不可逆操作在注入面前仍物理阻断**，这样被投毒的 agent 无法绕过 HITL 门执行破坏性动作。
20. 作为 Buzhou 接入方，我希望**以上能力都默认安全、可按机制开关**，这样我只引所需模块即得对应增强、无需全量启用。
21. 作为 Buzhou 接入方，我希望**新增行为有既有测试守护**（不改测试哲学），这样升级不破坏既有契约。
22. 作为贡献者，我希望**改动同步回写机制 Spec**，这样设计文档与实现不漂移、社区评审有据。

## Implementation Decisions

> 不含具体文件路径/代码片段（易过时）。接口级描述。Tier-1 = 本 Spec 建造范围；Tier-2/3 见「Out of Scope」。

### 范围与阶段

- 本 Spec = **Tier-1 跨四模块**（core / memory / spill / guard 各自最廉价高 ROI 项）。
- 既有的**真原创不重做、只强化**：并行工具+虚拟线程结构化关停、悬空调用 reactive 修复、微压缩 evidence-id、九段 P0–P3、动态预算、spill 三模回读、读写失败非对称、HITL session-state 授权、确定性事实采集。

### core（对应 wayfinder T12 Tier-1）

- **错误回喂模型**：在执行脊柱（`HarnessToolCallingManager` 所在的工具调用 fan-out 层），工具执行抛异常或工具缺失时，**合成一条 `ToolResponseMessage`**（内容=结构化错误文案 + 原工具入参），按 `tool_call` 原序回注模型，递归继续——而非上抛终结 Turn。与既有「失败转文本」（单工具超时 60s 失败转文本）统一为同一「错误即反馈」通道。来源：OpenAI Agents SDK `return_error_to_model` + `tool_error_formatter`。
- **有界 Turn 预算 + 可组合停止条件**：会话层（`AgentSession` 的 Turn 循环）引入每 Turn 的 think→tool 递归上界（默认值沿用业界保守值，可配）；超界走可插兜底 handler（产出优雅最终回复）。停止条件建模为**可组合 `Predicate<TurnContext>` 链**（预算 | 超时 | 工具信号 | 外部取消，支持 `&`/`|`）。来源：Vercel `stopWhen` / OpenAI `max_turns` / AutoGen 终止条件。
- 与 `buzhou-resilience`（模型韧性层：重试/统一超时/`onModelError`）的边界：错误回喂作用于**工具侧**异常；`onModelError` 作用于**模型侧**异常——两者正交、不互相吞掉。

### memory（对应 wayfinder T13 Tier-1）

- **事实对账（ADD/UPDATE/DELETE/NOOP）**：九段式摘要生成后，对每段候选事实跑一次对账 pass——与既有存储事实按语义近邻（embedding）比对，由模型裁决四态：新增 / 更新（并入互补信息）/ 删除（被新信息证伪）/ 不动。落点：摘要写侧 + SummaryStore 之上的事实索引。来源：Mem0。
  - 决策形状（来自 OSS 研究，非代码原型）：`ADD`（无语义等价）/ `UPDATE`（并入互补）/ `DELETE`（被证伪）/ `NOOP`（无变化）。
- **双时序事实有效性**：事实被取代时**标记失效（`valid_until`）而非物理删除**，保留 `valid_from`，支持时序回查与审计。来源：Zep/Graphiti + Mem0g。
- **预算拆解渲染给模型**：注入视图在九段每段末尾渲染 `chars_current / chars_limit` 页脚，让模型感知预算压力、自削 P3。来源：Letta memory blocks。
- **语义边界压缩触发**：暴露一个 `compact_now` 工具（MemoryModule 注册），模型可在任务边界/长草稿前自触发压缩；token 阈值保留为安全网（**双触发路径**：质量自触发 + token 兜底）。来源：LangChain Deep Agents。
- **增量摘要**：RunningSummary 跟踪 `summarized_message_ids` / `last_summarized_message_id`，只把**新消息**折入既有摘要，避免全量重摘要漂移。来源：LangMem `RunningSummary`。

### spill（对应 wayfinder T14 Tier-1）

- **hot-tail / cold-storage 两级保留**：近期 N 条工具结果全量内联（供推理），旧结果溢出至 evidence store，上下文只留占位符。"keep inline" 的条数/大小按会话可配。来源：Claude Code microcompaction。
- **per-tool durable override**：工具/Hook 可声明「永不溢出」或「超 X 才溢出」（对截断敏感的输出如 DB schema、整文件）。来源：Claude Code `maxResultSizeChars`。
- **自描述占位符**：溢出占位符（Reference Handle）统一含 **句柄 + 数据形状/schema 提示 + 字节/token 大小 + 精确回读动词与参数**，取代裸路径。来源：arXiv pointer-offloading + MCP results-widget。
- **token-aware 可配阈值**：溢出阈值可配且**按 token 计**（非硬编码字节/行），支持 per-tool override；任何截断必发**显式截断标记 + 回读句柄**，永不静默。来源：Codex 反面教材 + copilot-cli 静默损坏案例。

### guard（对应 wayfinder T15 Tier-1）

- **读侧 Spotlighting**：读侧 offload 把工具/RAG 输出回灌 prompt 时，用**随机分隔符 + 交织标记字符**包裹，system prompt 指示模型把标记段当**纯数据**（delimiting + datamarking）。来源：MSRC 间接注入防御。与既有「读侧失败降级透传」正交——这是对**内容可信度**的隔离，非失败处理。
- **canary 泄漏检测**：prompt 注入密语；`afterTool` 钩子检测密语是否泄漏进工具输出（`is_canary_leaked`）；泄漏则阻断该调用 + 把该输入 embedding 入拒识向量（**自硬化**）。来源：Rebuff。
- **`on_fail` 动词汇统一读写语义**：用 Guardrails 的失败动词作读写两侧统一语言——读侧默认 `FILTER`/`REFRAIN`（降级透传，映射既有「读降级」）、写侧默认 `EXCEPTION`（阻断，映射既有「写阻断」）、可恢复 schema 失败 `REASK`（错误回喂模型）。来源：Guardrails AI。这是给**既有读写失败非对称**套上业界心智模型，不改其语义。

## Testing Decisions

- **好测试只测外部行为，不测实现细节**；**优先复用既有接缝**，**尽可能用最高接缝**，理想只有**一个**端到端接缝。
- **主接缝（一个、最高）：端到端 agent session**——经 `examples` 集成测试用脚本化/反应式 `ChatModel` 驱动（既有：`RealBehaviorIntegrationTest`、`BuzhouDemoTest`、`SummaryEvaluationTest`）。仅断言外部行为：
  - 工具抛异常 → 模型收到错误反馈、Turn 完成（不死）；
  - 模型无限工具循环 → Turn 在预算内终止、产出优雅最终；
  - 超大工具输出 → 上下文出现自描述占位符 + 回读返回真实切片；
  - 工具输出含注入载荷 → 写侧工具调用**未**被影响（canary 未泄漏）；
  - 多轮后摘要 → 事实无重复/矛盾（评测式断言）；
  - 注入视图含每段预算页脚。
- **次接缝（仅在端到端不实用处，全部既有模块单测）**：`HarnessToolCallingManagerTest`（core 错误/超时/取消仍绿 + 新错误回喂）、`HookChainTest` / `HookEndToEndTest` / `LongContentGuardEndToEndTest`（guard 读侧隔离 + canary + on_fail 映射）、`DefaultMicroCompactorTest` / `SummaryEvaluationTest`（memory 对账/预算渲染/增量/双时序保真）、`SpillOffloadHookTest`（spill 两级保留/自描述占位符/可配阈值）。
- **先验**：上述既有测试即本 Spec 各断言的先验范式；评测式断言沿用 `SummaryEvaluationTest` 的方法论。
- 真实 LLM 行为由既有 gated 集成测试（`@EnabledIfEnvironmentVariable("BUZHOU_LLM_API_KEY")`）覆盖，本 Spec 不新增对真实 API 的强依赖。

## Out of Scope

- **Tier-2**（后续 Spec）：持久 run 注册表 + 枚举续跑（Mastra，把悬空修复升级为 proactive）、`FakeChatModel` + record/replay、显式 `CancelMode`、memory-as-tools（`search_evidence`/`revise_summary_section`）、向量 recall 三模搜、episodic few-shot、sleep-time 后台整理、spill 语义回读（第 4 模式）、AST-aware 切片、head+tail 回读风味、结构化 offload（structuredContent + 下载 URL）、内容寻址 handle 校验、Cedar 策略引擎作 HITL、分层分类器（Prompt-Guard 前置 + Llama-Guard 后置）、工具参数 schema 校验重试、`run_command` Firecracker 沙箱。
- **Tier-3**（远期）：事件溯源工具调用日志 + 幂等键（Temporal/Restate 级 crash-safe + exactly-once）、事务性并行批、time-travel/fork、HITL `interrupt` 按 toolCallId 匹配、FIDES 信息流控制、ECDSA 签名审计（IETF AAT）、CI 自动红队门（Promptfoo/PyRIT）。
- **非目标模块的「做深」**：`buzhou-observe-otel` / `buzhou-observe-dashboard` / `buzhou-mcp` / `buzhou-skills` / `buzhou-tools`（除 guard 侧 spotlighting/canary 触及的读侧路径外）维持现状。
- 发布到 Maven Central（已有 `RELEASING.md`）、`examples/` 超出既有 demo + 集成测试的扩展。

## Further Notes

- **落地记录（2026-08-13）**：Tier-1 十二项**全部落地**（wayfinder T16–T27 闭合；epic T12–T15 的 Tier-1 部分随闭、Tier-2/3 继续追踪）。机制 Spec 同步修订：`01-memory-compaction.md`（T23–T27 五项）、`02-spill.md`（T20–T22 三项）、`05-parallel-tools.md`（T16–T17 两项）、`07-hooks.md`（T18–T19 两项）。双轴 code-review（Standards + Spec）后修复三个交互缺陷：T18×T20（spotlight 包裹破坏 spill 形状识别 → `Spotlighting` 共享格式 + spill 解包裹）、T26（新版本 valid_from 未持久化）、T27（compact_now 绕过对账）。本地 `mvn -B -ntp clean verify` 16 模块全绿。
- **事实来源**：`docs/research/oss-best-of-breed.md`（wayfinder [T11](../../.wayfinder/tickets/T11-oss-best-ideas-core-memory-spill-guard.md)，4 并行 research 子 agent web 核验）；每项均标业界出处与 ROI。
- **执行切片**：wayfinder [T12 core](../../.wayfinder/tickets/T12-core-best-of-breed.md) / [T13 memory](../../.wayfinder/tickets/T13-memory-best-of-breed.md) / [T14 spill](../../.wayfinder/tickets/T14-spill-best-of-breed.md) / [T15 guard](../../.wayfinder/tickets/T15-guard-best-of-breed.md) 是本 Spec 的 per-module agent 工作切片（含 Tier-1/2/3 完整清单）。
- **刻度对齐**：wayfinder T3 已在「SPEC 判据满足」闭合；本 Spec 不推翻 T3，而是把杆抬到用户诉求的「对标开源最优」——不同刻度。
- **Spec 同步义务**：落地本 Spec 时须同步修订 `01-memory-compaction.md`（对账/双时序/预算渲染/语义触发/增量）、`02-spill.md`（两级保留/durable override/自描述占位符/token-aware 阈值）、`05-parallel-tools.md`（错误回喂/有界 Turn/可组合停止条件）、`07-hooks.md`（spotlighting/canary/on_fail 动词）。
- **语言与许可**：文档与注释主语言中文；坐标 `io.github.chyuan-cuihongyuan:buzhou-*`，Apache-2.0。
- **反模式（勿踩，详见研究文档各「反模式」节）**：仅静态 token 阈值触发压缩、非结构化单块摘要、规则截断回退、矛盾即删、全量重摘要、裸路径占位符、无标记静默截断、读写失败无差别对待、仅靠单一审核模型防注入、让模型自决命令是否需审批。
