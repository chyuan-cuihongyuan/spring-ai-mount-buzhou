# 各机制「对标开源最优」best-of-breed 技术萃取

> **来源**：wayfinder 研究票 [T11](../../.wayfinder/tickets/T11-oss-best-ideas-core-memory-spill-guard.md)，4 个并行 research 子 agent（core / memory / spill / guard）web 核验，2026-08-13。
> **用途**：喂 [T3 验收基线](../../.wayfinder/tickets/T3-depth-definition-of-done.md)（量化锚）与 [T9 Spring AI 边界文档](../spec/10-spring-ai-boundary.md)。在 [T2](../../.wayfinder/tickets/T2-spring-ai-native-vs-buzhou.md)（Spring AI 原生面）之外，补齐「业界已做到什么程度」。
> **口径**：萃取**思想/技术**（不抄实现），标注每项 best-in-class 出处与 Buzhou 适配路径；诚实区分「Buzhou 已领先 / 该采纳 / 该规避」。

---

## 0. 一句话结论

- **core**：并行工具（JDK21 虚拟线程）+ 悬空调用修复已领先 JVM 同类；要把「reactive 修复」升级为「proactive 恢复 + 事件溯源 + 幂等键」才算真正 crash-safe（对标 Temporal/Restate，非 LangGraph）。
- **memory**：微压缩 evidence-id（业界唯一确定性回读指针）、9 段分级优先级、动态预算 三件套已领先绝大多数同类（含 Spring AI / LangChain / Vercel / ADK）；该补 Mem0 的 ADD/UPDATE/DELETE/NOOP 事实对账、Letta 的「把预算压力渲染给 LLM」、语义边界触发。
- **spill**：byte/jsonpath/pagination 三模回读 + 读写失败非对称 + content-addressed evidence-id = **当前 SOTA**（无任何已投产框架同时具备）；该补 hot-tail/cold-storage 两级、per-tool durable override、自描述 stub、语义回读、AST-aware 切片。
- **guard**：读侧 offload / 写侧 onload **非对称** 与 **确定性事实采集（hook→state→attachment）** 两件是**真原创**——后者直接堵死 Letta/Unit-42 的「记忆投毒」攻击面；该补 MSRC spotlighting + Rebuff canary（读回路径间接注入）、Cedar 策略引擎（HITL 可分析化）、Guardrails `on_fail` 动词汇。

---

## 1. core（运行时编排）

### 1.1 现状最优 digest（取思想，不抄实现）

| 框架 | 可萃取思想 |
|---|---|
| **LangGraph** | 有状态图 + superstep；`interrupt()`/`Command(resume)` HITL；**superstep 事务性**（任一并行分支失败→整批回滚）；checkpointer + `get_state_history` time-travel/fork。**反模式**：resume 时节点从头重跑（`while True` 内 interrupt→指数重算）。 |
| **OpenAI Agents SDK** | `tool_not_found_behavior='return_error_to_model'` + `tool_error_formatter`（**错误回喂模型**）；`max_turns` + `error_handlers`；handoff；结构化 span（`agent/function/handoff/guardrail_span`）+ 可插拔 processor。 |
| **AutoGen v0.4** | Actor 模型；**可组合终止条件**（`MaxMessage/Timeout/External/Handoff`，`&`/`|`）；`CancellationToken` 贯穿每个 handler；`save_state/load_state` 跨运行续接。 |
| **Pydantic AI** | **最强结构化输出重试**：`result_validator` 抛 `ModelRetry`、per-turn/per-tool 重试预算、校验错误回喂；`TestModel/FunctionModel` + `capture_run_messages` 确定性单测。 |
| **Vercel AI SDK** | `stopWhen`（`isStepCount(20)` 默认等）；`MockLanguageModelV4`/`mockValues` 确定性 agent-loop 单测；`abortSignal` 取消。 |
| **Mastra** | workflow suspend/resume；**`listActiveWorkflowRuns()` + `run.restart()` + `restartAllActiveWorkflowRuns()`**（持久 run 注册表 + 枚举续跑）——TS 侧最强恢复面。 |
| **Aider** | 实证：**把代码编辑塞进 JSON tool-call schema 会降质**；用 unified-diff/纯文本 + 解析校验重试。 |

> **关键批判（Diagrid）**：checkpoint-only（LangGraph/CrewAI/ADK）**不是** durable execution——无 crash watchdog、无 exactly-once、无分布式锁、replay 会重跑 LLM。只有 **事件溯源 + 幂等键**（Temporal/Restate/Dapr）才是真 crash-safe。

### 1.2 该采纳（按 ROI 分级）

**Tier 1（廉价，天级）**
1. **错误回喂模型**（OpenAI Agents SDK）：工具异常→合成 `ToolResponseMessage`（错误文案 + 原 args）入历史，递归继续，而非整轮死。
2. **有界 turn 预算 + 可插拔停止条件**（Vercel/OpenAI/AutoGen）：每 Turn 限 think→tool 递归数；超限走可插拔 handler（优雅收尾）。把停止条件建模为可组合 `Predicate<TurnContext>`（预算|超时|工具信号|外部取消）。
3. **结构化 span**（OpenAI Agents SDK）：每个 think→tool 批次与每个并行工具发 Micrometer/OTel span，打 `toolCallId/turnId/sessionId`。

**Tier 2（1–3 周）**
4. **结构化输出/工具参数校验重试**（Pydantic AI）：args 对 schema 校验，失败→扣减 per-turn 重试预算、校验错误回喂、重调模型。
5. **持久 run 注册表 + 枚举续跑**（Mastra）：持久化在途 `Session/Turn` id + 末派发 `toolCallId`；重启时枚举续跑。**把现有 reactive 修复升级为 proactive 恢复——单笔最高价值中等投入。**
6. **确定性 FakeChatModel + record/replay fixture**（Vercel/Pydantic AI）：实现 Spring AI `ChatModel` 的假实现，按调用 index 回放录制响应（含并行工具调用请求），用于单测并行/修复/turn 语义。
7. **显式取消模式 + token 贯穿**（AutoGen/OpenAI）：定义 `CancelMode`（立即 / 当前工具后 / 当前 turn 后），token 透传嵌套工具链，防部分更新泄漏。

**Tier 3（深投入，高价值）**
8. **真 durable：事件溯源工具调用日志 + replay**（Temporal/Restate 金标准）：append-only 记 `(turnId, toolCallId, argsHash, status, result)`；replay 时已完成调用走缓存、在途/悬空调用按 **幂等键**（`argsHash` 或 client  supplied idempotency key）去重续跑。→ **唯一能让「crash-safe + exactly-once」从营销变真话的路径**。
9. **事务性并行批**（LangGraph）：并行工具结果先暂存，整批 + 模型重调成功才提交历史；失败回滚到批前 + 错误回喂。
10. **time-travel / fork（以 Completed-Turn 为检查点）**（LangGraph）：Buzhou 的 Completed-Turn 天然是干净检查点边界——比 LangGraph 的 per-superstep 更省。
11. **HITL `interrupt()`/`Command(resume)`（按 toolCallId 匹配）**（LangGraph）：避免其「resume 从头重跑」反模式。

### 1.3 Buzhou 已领先（诚实核验）
- **并行工具 + 超时/取消（虚拟线程）**：JVM 侧 genuinely best-in-class。LangGraph 跑的是并行*图分支*非单消息内并行*工具*；Spring AI 2.0 GA **顺序执行**；Vercel/OpenAI 并行但取消是 `abortSignal`/async-cancel，非 `StructuredTaskScope` 级结构化关停。**领先成立。**
- **悬空工具调用修复（history reload 时）**：比任何 checkpoint-only 框架都接近真 durable repair（它们**检测不到**派发未落库的调用、**无 exactly-once**）。诚实定调：对标 Temporal/Restate 是「逼近金标准」而非「超越」；Buzhou 是当前唯一做此事的 *agent harness*（非 workflow engine）。
- **Session/Turn/Completed-Turn 语义**：比同类的 flat messages/steps/actor-mailbox 更干净，天然是检查点/resume 单元 → 让 Tier3 #10 比 LangGraph 更省。

### 1.4 反模式 / 死路
- **checkpoint ≠ durable**：别仅凭 checkpoint 宣称 crash-safe（需事件日志 + 幂等键）。
- `interrupt()` 在 `while True` 内 → 指数重算。
- 工具异常上抛杀掉整轮 → 一律转「错误回喂」。
- 并发恢复无分布式锁 → 重复执行/状态损坏；多实例续同 session 需 lease/lock + 幂等键。
- 代码突变工具走 JSON schema 降质（Aider 实证）→ 用 diff/patch 载荷。
- 无界 `max_turns` → 必设上界。

---

## 2. memory（渐进式压缩）

### 2.1 现状最优 digest

| 框架 | 可萃取思想 |
|---|---|
| **MemGPT / Letta** | 三级（core/recall/archival，类 RAM/swap/disk）；**memory blocks**（`label/value/limit`，**字符计**，渲染 `chars_current/chars_limit` 让 LLM **看见自己的预算压力**）；**LLM 用工具自管记忆**（`core_memory_replace` 需**精确串匹配**、`archival_memory_search(page)`）；evict 前先生成**递归摘要**（建议每轮只 evict ~70% 保连续）；recall 支持 timestamp/text/embedding 三模搜。**sleep-time agent** 空闲后台整理记忆。 |
| **LangChain（classic，多已弃用）** | 词汇奠基：Buffer / Summary / **SummaryBuffer**（recent 原文 + 旧消息折入滚动摘要）/ TokenWindow（**只丢不摘**）/ VectorStoreRetriever。 |
| **LangGraph** | 短期=checkpointer（thread 级 super-step 持久）；长期=`Store`（`put/get/search(namespace,query,filter,limit)` + embedding 语义搜）；三类长期记忆（semantic/episodic/procedural）；`RemoveMessage`+`messagesStateReducer` 删消息；**LangMem `SummarizationNode`** 带 `RunningSummary`（`summarized_message_ids` 增量摘要）；JS `summarizationMiddleware`（`trigger.tokens` / `keep.messages`）。 |
| **LangChain Deep Agents** | **压缩作为 LLM 可调工具**——在**语义边界**（任务边界、长草稿前、需求失效时）自触发，非仅 token 阈值；默认 85% 容量触发；保留 ~10% 近况 + 压缩 tool-call 自身；用 trace 注入式 eval 验保守性。 |
| **LlamaIndex** | 新 `Memory` 类 + 可组合 block（`Static/Vector/FactExtraction`，每个带 `priority` + token ratio）。 |
| **Claude Code / Cline** | 接近容量**自压缩**（LLM 生成全面摘要，省 60–70% 窗口）；压缩前存**检查点**可回滚；失败回退**规则截断**（有损）；Cline ~80% 触发、已知会多烧 token / 高阈值丢关键上下文。 |
| **Zep / Graphiti** | **时序知识图谱**：实体/关系/事实带**双时序**（validity / transaction time），事实变更**标记失效而非删除**；LongMemEval 较 MemGPT +18.5% 准确、–90% 延迟。 |
| **Mem0** | 事实抽取 + **ADD/UPDATE/DELETE/NOOP 对账**（候选事实 vs top-K 语义近邻已有事实，LLM 裁决）；Mem0g 向量+图双栈；p95 延迟 –91%、token –90%+。 |

### 2.2 该采纳（按 ROI 分级）

**Tier 1（高价值，可直接采纳）**
1. **ADD/UPDATE/DELETE/NOOP 事实对账**（Mem0）：9 段摘要每段生成后跑对账 pass（embedding 近邻 + LLM 裁决），防重复/矛盾/陈旧——**最强去重/幂等技术**。
2. **语义边界触发（非仅 token %）**（LangChain Deep Agents）：给 LLM 一个 `compact_now` 工具，在任务边界自触发；token 阈值仅作安全网。双触发路径：质量（LLM 自触发）+ 安全（token 兜底）。
3. **双时序事实有效性（标记失效非删除）**（Zep/Mem0g）：9 段里事实被取代时，保留旧版 + `valid_until`，支持时序查询与审计、避免「断崖丢旧事实」。
4. **`summarized_message_ids` 增量摘要**（LangMem `RunningSummary`）：只把新消息折入既有摘要，避免全量重摘要的漂移累积。
5. **per-section 预算可见性**（Letta blocks）：9 段每段加 `chars_current/chars_limit` 页脚，让 LLM **自削 P3**——把 Buzhou 的动态预算**暴露给 LLM**。

**Tier 2（更重）**
6. **memory-as-tools（agent 自愈记忆）**（Letta）：暴露 `search_evidence(evidence_id)`（Buzhou 已有 evidence-id！）+ `revise_summary_section`，让 agent 纠正自己的压缩错误。
7. **跨 agent/线程共享记忆块**（Letta）：summary section 可「共享」同步（若 Buzhou 未来支持 sub-agent/并行 session）。
8. **向量 recall 三模搜**（MemGPT recall）：给已持久化的原始工具返回建 embedding 索引，支持 timestamp/text/embedding 回查**精确原文**。
9. **episodic memory 作 few-shot**（LangGraph）：成功工具序列存为 episode，新任务检索注入作示例。
10. **sleep-time 后台整理**（Letta）：turn 后异步重排 9 段优先级、对账、再压缩，保热路径快。

**Tier 3（廉价 wins）**
11. 每轮 evict ~70%（非 100%）保连续（Letta）。
12. 每次压缩前存检查点可回滚（Cline/Claude Code）。
13. 压缩保真度 eval（trace 注入需 pre-compaction 信息的 follow-up，测摘要是否保住答案）（Deep Agents）。

### 2.3 Buzhou 已领先（诚实核验）
- **微压缩 evidence-id 占位**：**业界领先**。MemGPT 推原文入 recall 但回查是模糊语义搜、**无确定性指针**；Claude Code/Cline 压缩后**直接丢原始工具结果**；LangGraph `RemoveMessage` **永久删**。Buzhou 的「确定性指针→持久化原文」在排障回读与正确性核验上**严格更强**——保留并强化。
- **9 段结构化摘要 + P0..P3 段内优先级**：**领先多数**。多数用单块非结构化滚动摘要；LlamaIndex block 有 `priority`、Letta block 有字符限，但 Buzhou 的**单一摘要制品内分级优先级**更细——预算压力下可**外科式削 P3 保 P0**。诚实 caveat：LlamaIndex/Letta 也有 block 级优先级，Buzhou 优势在「段内分级」。
- **动态预算（先扣后算）**：**领先 Spring AI / Vercel / ADK**。Spring AI 仅按条数窗口；LangChain TokenWindow 静态 token 限、不扣输出预留/工具 schema；LangMem/Deep Agents 静态阈值/百分比；ADK/Vercel **无原生预算计算**。仅 Claude API compaction 动态计输入 token 但不公开公式。Buzhou 的可组合公式**更透明可调**。**建议：把预算拆解渲染给 LLM**（见 Tier1 #5）。

### 2.4 反模式 / 死路
- 纯滑窗丢最旧（无恢复）= 渐进压缩的反面。
- 仅静态 token 阈值（85%）= 反应式，到点才仓促压缩。
- 非结构化单块摘要 = 无法选择性保高优先级。
- 规则截断回退 = 必然信息断崖（Buzhou 的微压缩+evidence-id 使其无必要）。
- 矛盾即删事实（丢时序）→ 应标记失效。
- 每次全量重摘要 → 漂移累积（须增量）。
- 100% 容量才触发 → 太晚、质量劣化。
- 把「压缩」与「记忆」混为一谈 → 生命周期不同，须分别管。

---

## 3. spill（溢出保护 / 大输出 offload / 回读）

> **关键发现**：**专用 first-class spill 机制极少**。多数仍做有损截断（head/tail）或整史压缩。最接近类比：Claude Code「microcompaction」、MemGPT/Letta OS 式 paging、一篇 2025-11 arXiv「pointer offloading」。**这种稀缺本身就是 Buzhou 占据稀疏生态位的证据。**

### 3.1 现状最优 digest

| 框架 | 可萃取思想 |
|---|---|
| **Claude Code** | **hot-tail / cold-storage**：近期结果全量内联（供推理），旧结果落盘、上下文只留 stub（"stored on disk, retrievable by path"）。MCP 阈值：>10K token 告警、默认硬上限 25K（`MAX_MCP_OUTPUT_TOKENS` 可调）；单输出可经 `_meta["anthropic/maxResultSizeChars"]` 标 **durable 至 ~500K 字符**。**回读=仅路径**（无 byte/line/jsonpath 协议——正是 Buzhou 填的缺口）。读写无差别对待。 |
| **OpenAI Codex CLI** | 硬编码 head+tail 窗口（256 行 **或** 10 KiB，先到先截，中段**销毁**）。无回读。经典有损截断，被指为 agent 静默造假的根因。 |
| **MCP（协议级）** | **分受众**：`content` 放 token 受限预览（行数/列名/样本/分页提示/**下载 URL**），`structuredContent` 放富 UI 载荷（**零 token**）；`poll_token` 句柄→换**一次性短命下载 token**（注意：临时句柄，**非**内容寻址完整性 id）。spec 明确建议客户端**校验并截断** sandbox 输出。现实静默损坏：copilot-cli >10KB 静默截断无提示。 |
| **Anthropic「Effective Context Engineering」** | 6 策略：①just-in-time（上下文只留轻量标识：路径/查询/链接，按需加载≈Buzhou 预览+指针哲学）；②选择性读大输出（head/tail/定向查询）；③**context clearing/editing**（删旧工具结果是「最安全、最轻」的压缩，删 tool_result 块、保缓存）；④结构化笔记外置（`NOTES.md`）；⑤sub-agent 隔离稠密上下文；⑥写侧工具须 token 高效、自含、错误鲁棒。 |
| **MemGPT/Letta** | OS 式 paging：main context（system + core memory + FIFO 历史+递归摘要）/ recall（全文）/ archival（无限读写溢出）。evict=**FIFO**（非 LRU/重要度）；agent 用工具自管（`archival_memory_search` 语义/串/时间戳，带 `page`）。`core_memory_replace` 须**精确串匹配**（结构性防静默覆写）。 |
| **arXiv「pointer offloading」(2511.22729, 2025-11)** | **最接近 Buzhou 的学术类比**：mirrored-tools = 输入 inspector（检测入参指针）+ 原工具 + 后处理器（offload 大输出、返回 memory-path 指针 + 数据形状描述）。回读=**分层 key-path**（dict 每对单独存，JSONPath-adjacent 但非全 JSONPath 语义）。**唯一带显式输入侧指针 inspector 的系统**——部分预见 Buzhou 写侧 onload 纪律。 |
| **Aider** | tree-sitter AST→符号图→PageRank 排序→token 预算切片（repo map，里程碑）；只读文件入图不可编；`.aiderignore`（gitignore 语法）降噪。**注意**：tree-sitter 对 >~32KB 文件会失败→**先切片再解析**。 |
| **LangChain splitters** | RecursiveCharacter（分隔符序）/ MarkdownHeader（按标题级）/ ExperimentalMarkdownSyntax（保空白+抽标题/代码块/水平线）/ HTMLSemantic / 代码语言感知——**语义块是比固定字节窗更好的回读单元**。 |
| **OpenAI File Search** | 上传自动分块 + HNSW + 重排，≤20 块、≤~16K token/查询——语义块回读。无 byte/jsonpath。 |

### 3.2 该采纳（按 ROI 排序）
1. **hot-tail / cold-storage 两级**（Claude Code）：近期 N 条全量内联、其余溢出至 evidence store；"keep inline" 数/大小按 session 可调。胜过单一全局阈值。
2. **per-tool durable override**（Claude Code `maxResultSizeChars`）：工具/hook 可声明「永不溢出」或「超 X 才溢出」——工具作者最知哪些输出截断是灾难性的（DB schema、整文件）。
3. **自描述 stub**（arXiv 论文 + MCP widget）：stub 一律含 **handle + 形状/schema 提示 + 字节/token 大小 + 精确回读动词与参数**。裸路径表现最差（模型忘回读/猜形状）。
4. **`page` 分页 + 多模态搜**（MemGPT/OpenAI）：Buzhou 已有分页；加**语义+串子选**让回读远聪明于纯字节窗 → 第 4 种回读模式：*semantic-chunk fetch*（溢出时 embed、回读时查）。
5. **code-AST-aware 切片边界**（Aider/LangChain）：源码按 AST 节点（函数/类/块）切，非代码回退 line/byte；**先切再解析**避 32KB cliff。
6. **head+tail 窗口作回读模式之一**（Codex）：作为 byte-range 的风味（`mode=byte, head=N, tail=M`）。
7. **结构化 offload（structuredContent + 下载 URL + 一次性 token）**（MCP）：若 Buzhou 渲染 UI，模型见预览+句柄、UI 见结构化载荷、批量下载走短命 token。
8. **精确匹配/结构性守卫**（MemGPT）：回读 handle **内容寻址**（校验切片 hash = 记录摘要）——强于 MemGPT 串匹配。
9. **just-in-time 标识纪律 + context-clearing**（Anthropic）：提供显式「清除/逐出已消费 handle」操作，保缓存。
10. **可配置、token-aware 阈值（非硬编码行数）**（Codex 公开 issue）：阈值可配且按 token 计（溢出时计 token 非仅字节），支持 per-tool override。

### 3.3 Buzhou 已领先（诚实核验）— **SOTA on 回读广度 + 非对称 + 完整性**
1. **三模回读（byte/jsonpath/pagination）无对手**：Claude Code 仅路径；MemGPT 分页+语义/串/时间（无 byte/jsonpath）；file search 仅语义块；Codex 仅 head/tail（且有损）；arXiv 论文分层 key-path（JSONPath-adjacent，学术原型未投产）。**Buzhou 的 byte+jsonpath+pagination 组合是 survey 中最广的回读面；JSONPath 回读基本独一无二。**
2. **读侧 lenient / 写侧 strict 非对称 = 真原创**：无任何 survey 框架区分读侧 offload（降级透传）与写侧 onload（失败阻断）。arXiv 论文有输入 inspector 但未 articulating 失败模式非对称；Anthropic「写侧工具设计」是 curation 非阻断。**Buzhou 的工程直觉正确**：半加载的*入参*产静默错结果（须阻断），降级的*读预览*仅降保真（可透传）。
3. **evidence-id（内容寻址 handle）严格更强**：MCP `poll_token` 一次性短命临时句柄（非完整性保证）；MemGPT 仅精确串匹配编辑。**无任何系统内容寻址溢出制品使回读能证明与原文一致。Buzhou evidence-id 是 survey 中最严谨的正确性保证。**

**诚实缺口（上表已覆盖）**：无 hot-tail/cold-storage 两级、无 per-tool durable override、无语义回读、无 AST-aware 切片、阈值 token-aware 可配置性待确认。

### 3.4 反模式 / 死路
- 硬编码 byte/line head+tail 截断（Codex）= 静默损坏教科书向量（模型把残文档当完整→编造中段）。Buzhou 须永不静默销毁。
- 无标记静默截断（copilot-cli #1732）→ 若 Buzhou 截预览，必发**显式截断标记 + 回读 handle**。
- 「抬高天花板」当策略（Gemini 4M 字符）= 仅推迟问题、推高成本/延迟。
- 把临时句柄当完整性保证（MCP `poll_token`）→ access token ≠ 「与我溢出的同字节」；evidence-id 与 access token 分离。
- 巨文件先解析再切片（tree-sitter >32KB 失败）→ 先切再解析。
- FIFO + 有损递归摘要逐出**工具制品**（精确字节重要）→ 逐出*句柄/stub*（廉价），制品留盘至显式清除，别摘要掉工具数据。
- 裸路径 stub（模型忘回读/猜形状）。
- 读写无差别对待（业界统一如此且是错的）→ 保非对称。

---

## 4. guard（Hook 护栏 / HITL / 防注入）

### 4.1 现状最优 digest

| 框架 | 可萃取思想 |
|---|---|
| **NeMo Guardrails** | proxy + 5 串行 rail：input → retrieval → dialog → execution → output；Colang（事件驱动 DSL）定义对话流；retrieval rail 过滤/消毒 RAG 上下文（**与读回间接注入直接相关**）；execution rail 校验工具入参/出参前后。弱点：非读写副作用非对称、非 state→attachment 渲染环。 |
| **Guardrails AI** | 输出校验 + RAIL schema + validator；**`on_fail` 动词**（crown jewel）：`REASK`（带校验错误回喂重提示）、`RETRY`、`FIX`、`FILTER`、`REFRAIN`、`EXCEPTION`；65+ hub validator（toxicity/PII/SQL 注入/regex/valid_choice…）。 |
| **Llama Guard 3** | 8B 内容审核分类器（MLCommons S1–S14 危害分类）；**model card 明示「可能被对抗/注入攻击绕过」**——对**间接注入/agentic 工具攻击检测弱**。正确组合：前面叠 **Prompt Guard**（注入/越狱检测）+ **Code Shield**。 |
| **Lakera Guard** | API「prompt-injection 防火墙」，宣称 98%+ 检测/<50ms/100+ 语言，含**间接注入（经 RAG/检索内容）**；闭源检测引擎（非 policy-as-code/不可自托管核心）。 |
| **Rebuff（Protect AI）** | **4 层自硬化注入检测**：①启发式（regex/子串）②LLM 检测 ③**VectorDB + Canary token**（注入密语、输出不应出现，泄漏即证被控）④JS 蜜罐。**canary 是 Buzhou 读回问题的关键技术**；泄漏则把恶意输入 embedding 入库阻断变体（自硬化）。 |
| **Cline / Roo / Kilo** | per-tool Allow/Ask/Deny；终端审批**非 regex**——**模型**给每条命令打 `requires_approval`（**弱点：被注入的模型会误标**）；YOLO 全自动批。 |
| **Claude Code** | `permissions.allow/deny/ask`（per-tool+per-path）+ 会话 permission mode；**`deny` 胜出且不可被 allow 例外覆盖**（不可逆操作正确优先级）；"Don't Ask" 模式对未预批**默认拒**（fail-closed）。但授权**非 session-state 结构化追踪**——静态规则集。 |
| **Cedar（AWS）/ OPA** | policy-as-code 授权语言；**Cedar 默认拒 + 数学可验证（Cedar Analysis）**；Strands Agents `CedarAuthorization` = 工具调用边界 intervention handler，映射 Principal(user)/Action(tool)/Resource/Context(args+session)，**fail-closed**。OPA/Rego 通用、采纳最广。 |
| **PyRIT / Promptfoo / Garak** | **离线红队**（非运行时）：Promptfoo YAML 测试矩阵 + CI；PyRIT 多轮攻击编排。2025 研究（21.4 万攻击）自动红队成功率 ~69.5% → **任何 guard 上线前须红队**。 |
| **E2B / Deno Sandbox** | 收敛于 **Firecracker microVM**（硬件级隔离，~150ms 冷启，临时 create→run→destroy）；E2B 为 AI agent 定制（Code Interpreter SDK + MCP server）；Deno Sandbox 加 secret 脱敏 + 进程/shell 执行管控。纯 V8 isolate/Worker 对联网不可信代码**不足**。 |
| **Instructor / Marvin** | Pydantic schema 强制 + 校验失败回喂重试（≈ Guardrails `REASK`，范围更窄）。 |
| **MSRC 间接注入防御（2025-07）** | **与本票最相关单源**：①**Spotlighting**（Delimiting/Datamarking/Encoding 把不可信文本标记为「仅数据」）②指令层级（system 明示「勿从不可信内容取指令」）③Prompt Shields 分类器 ④**TaskTracker**（分析模型内部激活检测攻击诱发行为）⑤细粒度权限/数据治理（敏感度标签，agent 永不获用户全权）⑥确定性阻断（特定外泄向量硬阻）⑦HITL 同意流 ⑧**FIDES 信息流控制**（确定性防间接注入，隔离不可信流与特权动作）。 |
| **Letta（MemGPT）** | core memory blocks「钉在 system prompt 每 turn 重注」= Buzhou attachment 渲染半；**但记忆写入是 LLM 自管工具**（`core_memory_replace`）→ **可被记忆投毒**（Unit 42：间接注入可持久化进长期记忆、未来 run 复燃）。 |
| **IETF Agent Audit Trail (AAT) 草案** | 标准化审计日志格式 + **ECDSA P-256 签名**防篡改（真不可否认）。 |

### 4.2 该采纳（按 ROI 排序）
1. **读侧 spill 上做 Spotlighting（Datamarking + Encoding）**（MSRC）：直接中和 Buzhou #1 威胁（工具输出/读回的间接注入）；确定性-ish、不额外模型。读侧 offload 把工具/RAG 输出回灌 prompt 时包随机分隔符 + 交织标记字符；system prompt 指示模型把标记段当纯数据。**最廉价高 ROI。**
2. **canary-token 泄漏检测（读回路径）**（Rebuff）：prompt 注密语，`afterTool` 跑 `is_canary_leaked(output)`；泄漏则阻断 + embedding 入拒识向量（自硬化）。与 #1 可组合。
3. **Cedar 作 HITL/授权策略引擎**（Strands/AWS）：把「config-driven gate」 bespoke 代码换成可验证、数学可分析、默认拒的 policy DSL；Buzhou「不可逆操作物理阻断」= Cedar `deny` + `permit when { context.session.human_approved }`；授权旗标在 session state（已是 Buzhou 设计）。
4. **`on_fail` 动词汇**（Guardrails AI）：读侧 validator 默认 `FILTER`/`REFRAIN`（降级透传）、写侧默认 `EXCEPTION`（阻断）、可恢复 schema 失败用 `REASK`（错误回喂）。完美映射读写非对称。
5. **FIDES 式信息流控制**（MSRC 研究）：读侧数据打 tainted 标签，`beforeTool/beforeModel` 强制 tainted 内容未经消毒/审批不得流入写侧工具调用——读写非对称的形式正确性终点。
6. **Firecracker microVM 沙箱化 `run_command`**（+ 网络出网 allowlist + secret 脱敏）（E2B/Deno）：最大爆炸半径工具的硬件级隔离。
7. **分层分类器组合：Prompt-Guard 类检测器前置 + Llama-Guard 类审核后置**（Meta + Promptfoo DB）：避 Llama Guard 间接/agentic 注入盲区。
8. **ECDSA 签名审计日志（AAT 格式）**（IETF 草案）：HITL 存在后，每个 allow/deny/ask + 工具调用带 principal/action/args/decision/签名——把 session-state 授权变成可审计证据。
9. **Pydantic schema 强制工具参数 + 失败重试**（Instructor/Guardrails）：`beforeTool` 校验 args；失败 `REASK`（轻）或 `EXCEPTION`（写侧）。
10. **CI 内自动红队门**（Promptfoo/PyRIT/Garak）：把 Promptfoo 红队跑作为 Buzhou guard 配置的 pre-release 门，回归断言攻击成功率。

### 4.3 Buzhou 已领先（诚实核验）
- **A. 读侧 offload / 写侧 onload 非对称 = 真原创框架**：无 survey 框架表达此。Cedar/Strands fail-closed 但**均匀**（无「读侧可 fail-open/降级」通道）；Guardrails `on_fail` 有*词汇*（FILTER vs EXCEPTION）但 validator **不绑副作用侧**。**Buzhou 贡献：把失败模式绑定到数据路径的副作用类别**（读=可降级、写=必阻断）。**建议：采用 Guardrails `on_fail` 动词作每条通道的统一语言。**
- **B. HITL 危险工具门 + session-state 授权 = 强组合，零件非全新**：Claude Code `deny`-wins、Cline Allow/Ask/Deny、Cedar `permit when { context.session.authorized }` 都有零件；但 Buzhou「授权是 session-state 事实、框架物理校验，非静态规则集、非 LLM 判断」**显著强于 Cline（模型决定 `requires_approval`）且比 Claude Code（静态规则）更干净**。**best-in-class 组合；采用 Cedar 作引擎使其也 best-in-class 验证。**
- **C. hook→state→attachment 闭环 = 确定性采集半真原创；渲染半 Letta 已有**：诚实拆分——「把采集事实渲染为 Attachment 下轮注入」**不新**（Letta core-memory blocks 每步重注即此；LangGraph channel+reducer 是基建版）；但「**hook 确定性采集（框架控，非 LLM 自管）**」**真原创且实质更优**——Letta LLM 自管写入（`core_memory_replace`）是已建档的[记忆投毒](https://unit42.paloaltonetworks.com/indirect-prompt-injection-poisons-ai-long-term-memory/)攻击面；Buzhou hook 在明确边界确定性采事实，**构造性抗注入**。**建议：明确框定为「确定性事实采集」vs「LLM 自管记忆」——这是可辩护区分，正对 Unit-42 投毒类。**

### 4.4 反模式 / 死路
1. 仅靠单一审核模型（Llama Guard）防注入（model card 自我否认；Promptfoo DB 证间接/agentic 弱）。
2. 让**模型**决定命令是否需审批（Cline `requires_approval`）→ 被注入模型误标绕过门；授权须框架决（Cedar/规则）。
3. LLM 自管长期记忆无 taint 追踪（vanilla Letta/MemGPT）→ 把注入变**持久**攻击；若让 LLM 写记忆，须 scope + 标 tainted。
4. 把读侧工具输出当可信指令（核心读回威胁）→ 须 spotlighting + canary +（理想）FIDES taint。
5. V8-isolate/容器-only 沙箱跑联网不可信代码 → 不足，用 Firecracker。
6. `on_fail=REASK` 无 `num_reasks` 上限 → 文档化生产失败模式（无限重提示/成本爆炸），限 1–2。
7. shell 命令静态 denylist（别名/引号/base64 轻易绕过）→ 沙箱 + Cedar 按*意图/资源*而非串匹配。
8. 闭源检测引擎（Lakera）作**唯一**控制（不可分析/不可自托管/安全关键路径锁定）→ 可作一层，永非唯一层。

### 4.5 专项：读回路径的间接 prompt 注入（spill/guard 交汇，Buzhou 最高风险面）
分层防御（按采纳顺序）：①**Spotlighting**（delimit+datamark，最廉价，MSRC）→ ②**canary 泄漏检测**（`afterTool`，Rebuff）→ ③检索/输入 rail 消毒检索上下文（NeMo）→ ④Prompt-Guard 类检测器 `beforeModel` 门 → ⑤**FIDES 信息流控制**（taint-label 读侧数据，未经审批不得达写侧工具，MSRC，形式正确性兜底，Buzhou 读写非对称的自然延展）→ ⑥写侧 HITL 门（Buzhou 已有，所有检测失败时的最后确定性阻断）。

---

## 5. 汇总：喂 T3 的「采纳 backlog」候选（按机制 × 优先级）

> 这是 T3 grilling 的**事实输入**：用户据此逐模块定「深到什么程度算 done」。每项标[来源]与[估值]。

| 机制 | Tier1（必做/廉价） | Tier2（应做/中等） | Tier3（冲极致/深） |
|---|---|---|---|
| **core** | 错误回喂模型[OAI]；有界 turn 预算+可组合停止条件[Vercel/AutoGen]；结构化 span[OAI] | 结构化输出重试[Pydantic]；**持久 run 注册表+枚举续跑**[Mastra]；FakeChatModel+record/replay[Vercel/Pydantic]；显式 CancelMode[AutoGen] | **事件溯源工具调用日志+幂等键**[Temporal/Restate]；事务性并行批[LangGraph]；time-travel(以 Completed-Turn 为点)[LangGraph]；HITL interrupt 按 toolCallId 匹配[LangGraph] |
| **memory** | ADD/UPDATE/DELETE/NOOP 对账[Mem0]；语义边界触发[Deep Agents]；双时序失效(标失效非删)[Zep]；增量摘要 `summarized_message_ids`[LangMem]；**预算拆解渲染给 LLM**[Letta] | memory-as-tools(暴露 `search_evidence`/`revise_summary_section`)[Letta]；向量 recall 三模搜[MemGPT]；episodic few-shot[LangGraph]；sleep-time 后台整理[Letta] | evict ~70%[Letta]；压缩前检查点[Cline]；压缩保真 eval[Deep Agents] |
| **spill** | hot-tail/cold-storage 两级[Claude Code]；per-tool durable override[Claude Code]；自描述 stub[arXiv+MCP]；**token-aware 可配阈值**[Codex 反面] | 语义回读(第 4 模式)[MemGPT/file-search]；AST-aware 切片(先切再解析)[Aider/LangChain]；head+tail 回读风味[Codex]；显式截断标记+handle | 结构化 offload(structuredContent+下载 URL+一次性 token)[MCP]；内容寻址 handle 校验[强于 MemGPT]；just-in-time+context-clearing[Anthropic] |
| **guard** | **读侧 Spotlighting**[MSRC]；**canary 泄漏检测**[Rebuff]；`on_fail` 动词汇作读写通道统一语言[Guardrails] | **Cedar 策略引擎**作 HITL[Cedar/Strands]；Prompt-Guard 前置+Llama-Guard 后置分层[Meta]；Pydantic args 校验+重试[Instructor]；`run_command` Firecracker 沙箱+出网 allowlist+secret 脱敏[E2B/Deno] | **FIDES 信息流控制**(taint label)[MSRC]；ECDSA 签名审计[AAT]；CI 自动红队门[Promptfoo/PyRIT] |

### 5.1 Buzhou 已领先清单（T3 须「保住并强化」而非重做）
- **core**：并行工具+虚拟线程结构化关停；悬空调用 reactive 修复（JVM 唯一 agent harness 做）；Session/Turn/Completed-Turn 语义。
- **memory**：微压缩 evidence-id（**业界唯一确定性回读指针**）；9 段 P0..P3 段内分级；动态预算（领先 Spring AI/Vercel/ADK）。
- **spill**：byte/jsonpath/pagination 三模回读（**survey 最广，JSONPath 基本独一无二**）；读写失败非对称（**真原创**）；content-addressed evidence-id（**最严谨完整性保证**）。
- **guard**：读写非对称失败语义（**真原创框架**）；HITL session-state 授权（强于 Cline/Claude Code 的组合）；**确定性事实采集**（hook→state→attachment，构造性抗注入，堵 Letta/Unit-42 记忆投毒）。

---

## 6. Sources（精选；子 agent 全量源见 T11）

**core** — Diagrid「checkpoints≠durable」(https://www.diagrid.io/blog/checkpoints-are-not-durable-execution-why-langgraph-crewai-google-adk-and-others-fall-short-for-production-agent-workflows) · LangGraph (https://docs.langchain.com/oss/python/langgraph/persistence , /interrupts , /graph-api) · OpenAI Agents SDK (https://openai.github.io/openai-agents-python/running_agents/ , /handoffs/ , /guardrails/ , /tracing/) · AutoGen (https://microsoft.github.io/autogen/stable/user-guide/agentchat-user-guide/tutorial/termination.html) · Pydantic AI (https://pydantic.dev/docs/ai/results/) · Vercel AI SDK (https://ai-sdk.dev/docs/ai-sdk-core/tools-and-tool-calling , /testing) · Mastra (https://mastra.ai/docs/workflows/overview) · Aider (https://aider.chat/2024/08/14/code-in-json.html)

**memory** — MemGPT paper (https://arxiv.org/pdf/2310.08560) · Letta (https://docs.letta.com/v1-sdk/memory/memory-blocks , https://www.letta.com/blog/agent-memory/) · LangGraph (https://docs.langchain.com/oss/python/langgraph/persistence , /concepts/memory) · LangMem (https://langchain-ai.github.io/langmem/reference/short_term/) · Deep Agents (https://www.langchain.com/blog/autonomous-context-compression) · LlamaIndex (https://www.llamaindex.ai/blog/improved-long-and-short-term-memory-for-llamaindex-agents) · Zep/Graphiti (https://arxiv.org/abs/2501.13956 , https://github.com/getzep/graphiti) · Mem0 (https://arxiv.org/html/2504.19413v1) · Claude Code compaction (https://platform.claude.com/docs/en/build-with-claude/compaction) · Cline (https://docs.cline.bot/features/auto-compact)

**spill** — Claude Code (https://code.claude.com/docs/en/mcp , https://decodeclaude.com/compaction-deep-dive/) · Anthropic context engineering (https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents , https://platform.claude.com/docs/en/build-with-claude/context-editing) · MCP client best-practices (https://modelcontextprotocol.io/docs/2026-07-28/develop/clients/client-best-practices) · Codex 截断 (https://github.com/openai/codex/issues/6426 , /5913) · copilot-cli 静默 (https://github.com/github/copilot-cli/issues/1732) · 静默损坏分析 (https://dev.to/gabrielanhaia/tool-result-truncation-the-silent-bug-that-makes-agents-lie-3epe) · MCP widget (https://futuresearch.ai/blog/mcp-results-widget/) · arXiv pointer offloading (https://arxiv.org/html/2511.22729v1) · Aider repomap (https://aider.chat/2023/10/22/repomap.html) · LangChain splitters (https://reference.langchain.com/python/langchain-text-splitters/markdown) · OpenAI file search (https://simonwillison.net/2024/Aug/30/openai-file-search/)

**guard** — NeMo (https://docs.nvidia.com/nemo/guardrails/about-nemo-guardrails-library/overview , https://arxiv.org/pdf/2310.10501) · Guardrails AI (https://guardrailsai.com/guardrails/docs/concepts/validator_on_fail_actions , /hub) · Llama Guard 3 (https://huggingface.co/meta-llama/Llama-Guard-3-8B) · Lakera (https://docs.lakera.ai/guard) · Rebuff (https://github.com/protectai/rebuff , https://www.langchain.com/blog/rebuff) · Cline (https://docs.cline.bot/features/auto-approve) · Kilo Code (https://kilo.ai/docs/getting-started/settings/auto-approving-actions) · Claude Code perms (https://kotrotsos.medium.com/claude-code-internals-part-8-the-permission-system-624bd7bb66b7) · Cedar (https://strandsagents.com/docs/user-guide/concepts/agents/interventions/cedar-authorization/ , https://aws.amazon.com/blogs/security/enforce-least-privilege-authorization-in-multi-agent-ai-chains-using-cedar/ , https://cedarpolicy.com/) · Promptfoo (https://www.promptfoo.dev/docs/red-team/agents/) · E2B (https://e2b.dev/) · Deno Sandbox (https://deno.com/blog/introducing-deno-sandbox) · Instructor (https://python.useinstructor.com/) · **MSRC 间接注入防御** (https://www.microsoft.com/en-us/msrc/blog/2025/07/how-microsoft-defends-against-indirect-prompt-injection-attacks) · LangGraph persistence (https://docs.langchain.com/oss/python/langgraph/persistence) · Letta stateful agents (https://docs.letta.com/v1-sdk/concepts/stateful-agents/) · **Unit 42 记忆投毒** (https://unit42.paloaltonetworks.com/indirect-prompt-injection-poisons-ai-long-term-memory/) · IETF AAT (https://datatracker.ietf.org/doc/draft-sharif-agent-audit-trail/)
