# 参照系实现调研素材（Spring-Ai-Trip 设计 Spec 用）

> 调研日期：2026-07-31。所有结论标注一手来源。用途：为携程文章留白处（动态预算、微压缩、九段式摘要、Spill、认知可观测、Skill、MCP 热插拔）的自主推演提供业界参照。

## TL;DR

- **Claude Code** 是「微压缩 + 结构化摘要 + 压缩后显式恢复」三条机制的直接原型：其泄漏/逆向资料显示存在三层压缩（MicroCompact → Session Memory Compact → Full Compact），compact prompt 恰为**九段结构**，auto-compact 阈值 = 有效窗口 − 13K 缓冲（社区观察到约 95%），压缩后按预算重新注入最近读过的 5 个文件（50K token 预算）与 CLAUDE.md。
- **AgentScope Java（阿里 Tongyi Lab）** 与蓝文几乎同构：明确提出 **eviction 治"宽度"、compaction 治"深度"** 的正交分层，`ToolResultEvictionMiddleware` 阈值 80K 字符、首尾各 2K 预览 + `read_file` 路径回读，摘要默认 prompt 为 `SESSION INTENT / SUMMARY / ARTIFACTS / NEXT STEPS` 四段，压缩前先 flush 长期记忆（双层：`memory/YYYY-MM-DD.md` 流水 + `MEMORY.md` 策划层）。
- **LangChain4j** 提供 Java 侧最基础的抽象锚点：`ChatMemory`（驱逐策略容器）/ `ChatMemoryStore`（三方法持久化 SPI）/ `TokenCountEstimator`（jtokkit 本地估算），但无压缩、无摘要——正是 Harness 层的空档。
- 九段式摘要可将 Claude Code 九段逐段映射为候选模板，并按"恢复执行所必需 vs 背景信息"标 P0–P3。

---

## 1. Claude Code 的上下文管理

### 1.1 三层压缩体系（逆向源码分析）

来源：openedclaude/claude-reviews-claude《Episode 11: The Compaction System》
https://github.com/openedclaude/claude-reviews-claude/blob/main/architecture/11-compact-system.md

| 层 | 机制 | 触发 | 压缩率 | 缓存影响 |
|---|---|---|---|---|
| **MicroCompact** | 清除旧工具结果 | 每轮（按时间/条数） | ~10–50K tokens | 暖缓存走 `cache_edits` API 不重建；冷缓存直接改写内容 |
| **Session Memory Compact** | 用后台维护的 session memory 替换旧消息 | auto-compact 阈值 | ~60–80% | 失效但**不调 LLM** |
| **Full Compact** | LLM 摘要全量对话 | auto-compact 或手动 `/compact` | ~80–95% | 失效 + 1 次 API 调用 |

关键实现细节：

- **MicroCompact** 只针对高产可复现的工具结果：`FileRead / Bash / Grep / Glob / WebSearch / WebFetch / FileEdit / FileWrite`；AgentTool、MCP 工具结果不动。占位文案为 `[Old tool result content cleared]`，保留最近 N 条。token 估算为字符遍历后 ×4/3 保守放大，图片/PDF 按 2,000 token 定额。
- **Session Memory Compact** 保留策略：`minTokens=10K`、`minTextBlockMessages=5`、`maxTokens=40K`，从最近一条已摘要消息向前扩展；`adjustIndexToPreserveAPIInvariants()` 保证 tool_use/tool_result 不拆对、同 message.id 的流式消息不拆组。
- **Full Compact 管线**：PreCompact hooks → 剥图片 → 剥 reinjected 附件（skill 清单等）→ 流式摘要（带 prompt-too-long 重试，最多 3 次、每次按 API round 分组丢头部）→ `formatCompactSummary()` 剥掉 `<analysis>` 草稿 → 清文件状态缓存 → **压缩后上下文恢复** → 重跑 SessionStart hooks → PostCompact hooks。
- 熔断：`MAX_CONSECUTIVE_AUTOCOMPACT_FAILURES = 3`，连续失败 3 次本会话停止 auto-compact。
- Partial compact 支持 `'from'`（保留前缀，缓存友好）与 `'up_to'` 两个方向。

### 1.2 auto-compact 触发阈值

同上新源码分析：

```ts
effectiveWindow = contextWindow - min(maxOutputTokens, 20_000)
autoCompactThreshold = effectiveWindow - 13_000   // AUTOCOMPACT_BUFFER_TOKENS
```

200K 窗口模型：有效窗口 ≈ 180K，auto-compact ≈ 167K（约 83.5%）。另有三档告警水位：warning（有效窗口−20K）、blocking limit（有效窗口−3K，必须手动 compact）。社区普遍观察到"约 95% 才触发"（指相对原始窗口的显示口径），并认为过晚、有 issue 要求可配置：

- GitHub Issue #15719《Configurable Context Window Compaction Threshold》：https://github.com/anthropics/claude-code/issues/15719
- MindStudio 实践建议 60% 主动 compact：https://www.mindstudio.ai/blog/claude-code-compact-command-context-management

### 1.3 compact 摘要 prompt（九段结构，逆向全文）

来源：Yuyz0112/claude-code-reverse（compact.prompt.md）
https://github.com/Yuyz0112/claude-code-reverse/blob/main/results/prompts/compact.prompt.md

结构要点：

1. 先让模型把分析写进 `<analysis>` 标签（按时间顺序逐消息复盘：用户显式请求、采取的方法、关键决策、文件名/完整代码片段/函数签名/文件编辑、错误与修复、用户纠偏反馈），注入前**剥掉**——"先想后写、想不占预算"。
2. 再按固定九段输出 `<summary>`：
   1. Primary Request and Intent（用户显式请求与意图）
   2. Key Technical Concepts（技术概念/框架清单）
   3. Files and Code Sections（逐文件：为何重要、改了什么、完整代码片段）
   4. Errors and fixes（错误 + 修法 + 用户纠偏反馈）
   5. Problem Solving（已解决问题与进行中的排障）
   6. All user messages（**全部**非工具结果的用户消息，追踪意图漂移）
   7. Pending Tasks（显式要求的待办）
   8. Current Work（摘要请求前的即时工作现场，含文件名/代码）
   9. Optional Next Step（下一步，须与用户显式请求严格对齐，附最近对话**原文引用**防漂移）
3. 支持用户自定义 compact 指令追加（`/compact 关注测试输出` 这类）。

### 1.4 压缩后的显式恢复（post-compact restore）

来源同 1.1：

```ts
POST_COMPACT_MAX_FILES_TO_RESTORE = 5
POST_COMPACT_TOKEN_BUDGET = 50_000
POST_COMPACT_MAX_TOKENS_PER_FILE = 5_000
POST_COMPACT_SKILLS_TOKEN_BUDGET = 25_000
POST_COMPACT_MAX_TOKENS_PER_SKILL = 5_000
```

压缩后自动重注入：最近读过的 top-5 文件、调用过的 skills、活动 plan 内容、plan mode 指令、deferred tool deltas、agent 清单 deltas、MCP 指令 deltas。Anthropic 官方博客亦确认"压缩上下文 + 最近访问的 5 个文件"（见 1.6）。

### 1.5 CLAUDE.md 的角色（官方文档）

来源：Claude Code Docs《How Claude remembers your project》
https://code.claude.com/docs/en/memory

- 双轨记忆：**CLAUDE.md**（人写的持久指令）+ **Auto memory**（Claude 自己记的笔记，存 `~/.claude/projects/<project>/memory/`，`MEMORY.md` 作索引，仅加载前 200 行/25KB，主题文件按需读）。
- CLAUDE.md 在**每次会话开始全量注入**（以 user message 形式、非 system prompt）；沿目录树向上收集并拼接；子目录 CLAUDE.md 在读到该目录文件时才按需加载；支持 `@path` 导入（最深 4 跳）；块级 HTML 注释注入前剥离。
- 建议每文件 <200 行，超长用 `.claude/rules/` + `paths` frontmatter 按路径按需加载。
- **与压缩的关系（"What survives compaction"）**：项目根 CLAUDE.md 在 `/compact` 后从磁盘重读并重新注入；嵌套 CLAUDE.md 不自动重注入，下次读到对应目录文件时再加载。

### 1.6 Anthropic 官方方法论

《Effective context engineering for AI agents》（2025-09-29）
https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents

- context rot：token 增多 → 检索精度下降，上下文是有边际收益递减的有限资源。
- 长程任务三件套：**Compaction**（高保真蒸馏，"先保 recall 再调 precision"；最安全轻量的形式是 tool result clearing）、**Structured note-taking**（agent 自主写 NOTES.md 式持久记忆，跨摘要步骤保持连贯）、**Sub-agent 架构**（子代理烧几万 token 只回 1–2K 蒸馏结论）。
- Claude Code 的混合策略：CLAUDE.md 前置静态注入 + glob/grep 等原语 just-in-time 检索（progressive disclosure）。
- 配套平台能力：context editing（工具结果清理）与 memory tool（文件式记忆），见 https://www.anthropic.com/news/context-management

---

## 2. AgentScope Java（agentscope-ai/agentscope-java，阿里 Tongyi Lab）

### 2.1 四套正交策略（官方中文文档）

来源：AgentScope Java v2 文档《上下文压缩》
https://java.agentscope.io/v2/zh/docs/harness/compaction.html

| 策略 | 解决问题 | 触发时机 | 中间件 |
|---|---|---|---|
| 对话摘要压缩 | 上下文太"**深**"——消息条数/token 累计过多 | 每次模型推理前 | `CompactionMiddleware` |
| 大工具结果卸载 | 上下文太"**宽**"——单条工具结果过大 | 工具执行后 | `ToolResultEvictionMiddleware` |
| 上下文溢出兜底 | 真撞 `context_length_exceeded` | `call()` 抛错时 | `HarnessAgent.recoverFromOverflow` |
| 预压缩参数截断 | `write_file` 等大入参事后没人看 | 摘要前轻量预处理 | `CompactionConfig.TruncateArgsConfig` |

四套策略正交、可任意组合、默认全关。溢出兜底 = 强制 `triggerMessages=1` 极端压缩后自动重试一次。压缩只动 `AgentState.contextMutable()` 的对话消息列表；Plan Mode 状态、子 agent 后台任务、todo 清单、权限规则均不受影响。原文永不压缩落 `sessions/<id>.log.jsonl`，agent 可用 `session_list / session_history / session_search` 自查。

### 2.2 "宽度 vs 深度"分层论述出处

- 官方文档（上文链接）：摘要压缩治"深"、大结果卸载治"宽"。
- Zread 源码导读《Memory Compaction and Eviction》明确"**dual-axis memory management system: compaction (depth) + eviction (width)**"：
  https://zread.ai/agentscope-ai/agentscope-java/18-memory-compaction-and-eviction

### 2.3 ToolResultEvictionMiddleware 设计细节

来源同上（Zread 18 + 官方 memory 文档 https://java.agentscope.io/v2/zh/docs/harness/memory.html ）：

- 阈值：`maxResultChars` 默认 **80,000 字符 ≈ 20K tokens**。
- 落盘位置：工作区相对路径 `large_tool_results/`（确定性路径）。
- 占位符：上下文内 `ToolResultBlock` 替换为**首+尾各 2,000 字符**预览 + "完整内容见 `{path}`" 提示；模型想看全文自己 `read_file`（回读）。
- 排除清单：`read_file / write_file / edit_file / grep_files / glob_files / list_files / memory_search / memory_get / session_search`（自带分页或返回值小；且 `read_file` 排除可避免回读完又被卸载）。**Shell `execute` 故意不排除**——命令输出可能极大。
- 触发独立：只看单条结果体量，与消息总数无关。

### 2.4 Compaction 细节（ConversationCompactor）

来源同 Zread 18：

- **触发**：`triggerMessages`（默认 50）或 `triggerTokens`；`triggerTokens=0` 为动态模式 = `model.contextWindow − reserved(20K)`，模型不报窗口则回落 160K。
- **keep 三模式**：静态（>0 固定 token 预算）/ 条数（=0 用 `keepMessages`，默认 20）/ 动态（−1：`min(8K, max(2K, usable*0.25))`）。二分查找定 cutoff，`findSafeCutoffPoint()` 保证 ASSISTANT tool_call 与 TOOL result 不拆对。
- **摘要消息形态**：以 USER 消息注入、`name="__compaction_summary__"`；带 offload 文件路径引用；ID 由内容 `UUID.nameUUIDFromBytes()` 派生（幂等）。**默认摘要 prompt 四段：`SESSION INTENT / SUMMARY / ARTIFACTS / NEXT STEPS`**（官方 compaction 文档）。
- **压缩前两步轻量预处理（不走 LLM）**：参数截断（`maxArgLength` 默认 2,000，后缀 `...(argument truncated)`）；聚合工具结果剪枝（保护最近 40K tokens 工具输出，超出部分首+尾 2,000 字符 + `... (N chars pruned) ...`）。
- **压缩前联动**：`flushBeforeCompact`（默认开，先抽事实入长期记忆）+ `offloadBeforeCompact`（默认开，原文写 JSONL）。flush 只处理新压缩的原始消息（`filterSummaryMessages()` 防重复抽取）。
- **token 估算**：`TokenCounterUtil` 保守比率 **2.5 字符/token**（中英混排）+ 结构开销常数（消息 +5、工具调用 +10、工具结果 +8、多媒体 +5）。
- **容错**：每步降级不炸 turn——flush 失败记警告继续；offload 失败退回无文件引用格式；摘要 LLM 失败用 `"(Summary unavailable)"` 兜底；cutoff=0 时静默跳过。

### 2.5 双层长期记忆 + ReMe/Mem0 对接形态

来源：官方 memory 文档 + ReMe 集成文档 https://java.agentscope.io/v2/zh/integration/memory/reme.html

- **双层文件记忆**：第一层 `memory/YYYY-MM-DD.md` 只追加流水账（Flush LLM 调用写入）；第二层 `MEMORY.md` 由 Consolidation LLM 周期性（默认 30min）合并去重整体重写（上限默认 4,000 tokens），每轮推理注入 system prompt。后台任务还负责 90 天归档 daily、180 天清 session JSONL。agent 工具：`memory_search`（关键词扫，≤30 条命中）、`memory_get path startLine endLine`（**范围读取**）。
- 记忆管线三处独立 LLM 调用（flush / consolidation / compaction summary）各有独立 prompt，且各自支持 `.model(...)` 换便宜模型。
- **LongTermMemory SPI 对接外部记忆服务**：`ReMeLongTermMemory`（`agentscope-extensions-reme`）以 `userId` 映射 ReMe `workspace_id`；写入时把过滤后的对话拼成 `ReMeTrajectory` 整体送 `add` 接口由服务端 LLM 抽取；检索以当前消息为 query 调 `search`，优先返回聚合 answer。过滤策略：只留 USER/ASSISTANT、跳过含 `ToolUseBlock` 的助手消息、跳过 `<compressed_history>` 标记的压缩历史。模式 `LongTermMemoryMode.BOTH`（自动控制 + agent 主动工具）。同类集成还有 **Mem0**、**百炼记忆**（见文档侧边栏 integration/memory/*）。

---

## 3. LangChain4j

来源：官方文档 Chat Memory 教程 https://docs.langchain4j.dev/tutorials/chat-memory ；源码经 Context7/zread 核验。

- **`ChatMemory`**：消息容器（`id() / add() / messages() / clear()`），附加驱逐策略、持久化、SystemMessage 特殊处理。官方明确区分 **Memory ≠ History**：Memory 是"给 LLM 看的信息子集"，可驱逐、可摘要、可改写；History 是完整事实记录，需自行维护。
- **两个内置驱逐实现**：`MessageWindowChatMemory`（按条数滑窗，适合原型）；`TokenWindowChatMemory`（按 token 滑窗，消息不可分割、放不下整条驱逐；支持 `dynamicMaxTokens(Function<Object,Integer>, TokenCountEstimator)` 动态预算）。
- **`ChatMemoryStore` SPI**：三方法 `getMessages(memoryId) / updateMessages(memoryId, messages) / deleteMessages(memoryId)`；`updateMessages` 每次加消息时调用（一轮通常两次：UserMessage、AiMessage），**被驱逐的消息也会从 Store 同步驱逐**（滑窗记忆不提供历史留存）。序列化用 `ChatMessageSerializer/Deserializer`（JSON）。
- **特殊处理**：SystemMessage 恒保留、同时只有一条、同内容忽略、不同内容替换（可配 `alwaysKeepSystemMessageFirst`）；若含 `ToolExecutionRequest` 的 AiMessage 被驱逐，其孤儿 `ToolExecutionResultMessage` 连带驱逐（OpenAI 等禁止孤儿工具结果）——即"不拆工具调用对"的 LangChain4j 表达。
- **token 估算**：现行为 `TokenCountEstimator` 接口（`estimateTokenCountInText / estimateTokenCountInMessage(s)`）。实现：`OpenAiTokenCountEstimator`（jtokkit 纯 Java 本地跑，o*/gpt-4.*/gpt-5 用 O200K_BASE，其余按模型名查编码）、`AnthropicTokenCountEstimator`（走 API）、`HuggingFaceTokenCountEstimator`。旧 `Tokenizer` 接口已演进到该命名。
- **记忆注入时机（AiServices）**：每次服务方法调用时，由 `ChatMemoryService`/`DefaultChatMemoryService` 按 `@MemoryId` 取（或新建）`ChatMemory`，把 memory 中全部消息与当前 `@UserMessage` 一起组装发给模型，响应写回 memory。多用户隔离用 `@MemoryId` + `ChatMemoryProvider.get(memoryId)`。示例：https://github.com/langchain4j/langchain4j-examples
- **对 Spring-Ai-Trip 的启示**：LangChain4j 止于"滑窗 + 持久化 SPI"，无摘要、无压缩、无工具结果治理——Harness 层（渐进压缩/微压缩/Spill）正是其之上的空白带。

---

## 4. 大厂公开实践

### 4.1 携程

- 蓝本文（本项目 CONTEXT 所指文章）是携程在 Agent 运行时上下文管理上最系统的公开材料。其余公开材料偏产品/算法层：
  - 携程度假智能客服技术分享（NLU、DST、多轮 Task Bot）：https://cloud.tencent.com/developer/article/1537277
  - 携程商旅"七大 Agent"差旅生态（产品形态）：https://ct.ctrip.com/thinktanks/258182590517409
  - TripGenie × Azure OpenAI 合作案例（微软官网）：https://www.microsoft.com/zh-cn/customers/story/18763-ctrip-computer-technology-github
  - 语义匹配在携程智能客服的应用：https://zhuanlan.zhihu.com/p/452046671
- 结论：携程系在"上下文压缩/工具结果治理"上的公开细节基本只有蓝本文，留白处需以 Claude Code / AgentScope 参照推演。

### 4.2 阿里

- **AgentScope Java**：见第 2 节（Harness 四策略 + 双层记忆 + ReMe/Mem0 集成）。另有综述文：《AgentScope Java 核心架构深度解析》https://www.cnblogs.com/wasp520/p/19385021
- **Spring AI Alibaba**：
  - 短期记忆作为 Graph/Agent 状态的一部分管理，checkpoint/saver（MemorySaver、RedisSaver、MySQL 等）持久化会话状态：https://java2ai.com/docs/frameworks/agent-framework/tutorials/memory ；https://developer.aliyun.com/article/1726596
  - 长期记忆走 `Store` 抽象（跨对话存用户/应用级数据）：https://java2ai.com/docs/frameworks/graph-core/core/memory
  - 机制综述（掘金）：《一站式了解 Spring AI Alibaba 的 Memory 机制》https://juejin.cn/post/7596990822127566886
- 要点：SAA 的记忆是"状态持久化 + 检查点续接"思路（LangGraph 谱系），摘要/压缩层面公开实现较弱，与蓝文 Harness 定位互补。

### 4.3 字节

- 火山引擎开发者社区《Agent 架构综述：从 Prompt 到 Context》：https://developer.volcengine.com/articles/7542489492871987254 —— 从战术性 prompt 构建转向战略性上下文架构的综述。
- 扣子（Coze）/HiAgent 在上下文管理上的**一手工程细节公开很少**，多为产品发布稿；可引用字节系综述文章佐证"上下文工程成为平台级关注点"。

### 4.4 腾讯

- 腾讯云社区《为 AI agent 构建智能上下文记忆》：https://cloud.tencent.com/developer/article/2667163 —— rolling window（按条数/token）、倒排索引裁剪、语义检索、语义经验记忆、GraphRAG 五段式方案谱系。
- 《AI Agent 记忆系统：从短期到长期的技术架构与实践》（腾讯新闻转载）：https://view.inews.qq.com/a/20260130A01VTD00 —— 短/长期记忆分层、record & retrieve 两流程、AgentScope AutoContextMemory 等业界盘点。

### 4.5 其他可参考

- AWS《Agentic AI 基础设施实践系列（九）：Context Engineering》：https://aws.amazon.com/cn/blogs/china/agentic-ai-infrastructure-practice-series-nine-context-engineering/
- Datawhale hello-agents 第九章《上下文工程》：https://github.com/datawhalechina/hello-agents

---

## 5. 九段式摘要的业界近似物与候选段落清单

### 5.1 候选结构来源

1. **Claude Code compact prompt = 天然九段**（全文见 1.3）：Primary Request & Intent / Key Technical Concepts / Files & Code Sections / Errors & Fixes / Problem Solving / All User Messages / Pending Tasks / Current Work / Optional Next Step。配套技巧：`<analysis>` 草稿注入前剥离、第 9 段附原文引用防任务漂移、支持用户自定义追加指令。
2. **AgentScope Java 默认摘要 prompt 四段**：`SESSION INTENT / SUMMARY / ARTIFACTS / NEXT STEPS`（面向工程/编排类 agent；来源：官方 compaction 文档）。
3. **MemGPT/Letta memory blocks**：core memory = 常驻上下文的可编辑块（典型块：persona / human / task），每块有 label + description + value + **字符上限**；agent 用工具自改写；archival（向量/图库）与 recall（完整历史）在外部。来源：https://www.letta.com/blog/agent-memory/ ；https://www.letta.com/blog/memory-blocks/
4. **Mem0**：非摘要结构，而是**事实级管线**——从对话抽取候选事实 → 与存量记忆比对 → ADD / UPDATE / DELETE / NOOP 决策（OSS v3 改为单遍 ADD-only）。来源：Mem0 论文 https://arxiv.org/html/2504.19413v1 ；https://docs.mem0.ai/migration/oss-v2-to-v3 ；https://mem0.ai/blog/memory-eviction-and-forgetting-in-ai-agents
5. **Anthropic 官方摘要取舍建议**：保 architectural decisions / unresolved bugs / implementation details，弃 redundant tool outputs；调摘要 prompt 时"先最大化 recall，再迭代 precision"（来源 1.6）。
6. **Letta 驱逐建议**：每次只驱逐部分（如 70%）消息保持连续性；被驱逐消息做**递归摘要**（旧摘要与新消息一起再摘要，越早的内容权重越低）。

### 5.2 候选段落清单（供九段模板 + P0–P3 推演）

| 候选段落 | 来源 | 建议优先级逻辑 |
|---|---|---|
| 用户核心诉求 / 会话意图（Primary Request & Intent；SESSION INTENT） | CC-1、AS-1 | **P0 死保**——丢意图则后续全错 |
| 当前工作现场（Current Work：正在改的文件/函数/代码片段） | CC-8 | **P0**——恢复执行的最小必要集 |
| 下一步（Next Step，附最近对话原文引用） | CC-9、AS-NEXT STEPS | **P0/P1**——含原文引用防漂移 |
| 待办任务清单（Pending Tasks） | CC-7 | P1 |
| 错误与修复 + 用户纠偏反馈（Errors & Fixes） | CC-4 | P1——避免重蹈覆辙，用户纠偏权重最高 |
| 关键产物（Files & Code Sections / ARTIFACTS：文件路径、签名、为什么重要） | CC-3、AS-ARTIFACTS | P1/P2——可用 evidence 指针替代全文 |
| 已解决问题与排障进展（Problem Solving） | CC-5 | P2 |
| 关键技术概念/决策（Key Technical Concepts；架构决策） | CC-2、Anthropic 取舍建议 | P2 |
| 全部用户消息清单（All User Messages） | CC-6 | P2/P3——长会话下最先降级为"最近 N 条 + 指针" |
| 背景/领域上下文（Letta human/task 块式偏好、项目约定） | Letta blocks、CLAUDE.md | 不应放摘要里——放 CLAUDE.md/MEMORY.md 式常驻层，压缩后重注入 |

优先级推演规则：**恢复"接下来要做什么"所必需的 = P0**（意图、现场、下一步）；**避免返工/犯错的 = P1**（待办、错误与纠偏、产物清单）；**背景与历史细节 = P2/P3**（概念、全量用户消息、过程叙事），可降级为 evidence-id 指针 + 回读路径。与蓝文"信息从高精度原文连续降级到高密度摘要、永不断崖丢弃"对齐：P3 段落不是删除，而是换成指针（对应 AgentScope 的 offload JSONL + `session_search` 回读、Claude Code 的 MicroCompact 占位符）。

---

## 6. 对蓝文其他留白点的参照映射

| 蓝文机制 | 最近参照 |
|---|---|
| 微压缩（纯内存、不调 LLM、占位符带 evidence-id） | Claude Code MicroCompact（占位符 + 保留最近 N 条）；AgentScope 聚合剪枝（protectTokens=40K + `... (N chars pruned) ...`）+ offload JSONL（evidence 落点） |
| Spill 落盘 + 范围回读 | AgentScope ToolResultEviction（`large_tool_results/` + 首尾预览 + `read_file` 回读；`memory_get startLine/endLine` 行范围读取是现成的范围读取 API 形态） |
| 动态预算"先扣后算" | Claude Code `effectiveWindow = window − min(maxOutput,20K)`、auto-compact −13K 缓冲；AgentScope `triggerTokens = contextWindow − reserved(20K)` 动态模式 |
| 悬空调用修复 | AgentScope `findSafeCutoffPoint()`（不拆 ASSISTANT/TOOL 对）；LangChain4j 孤儿 ToolExecutionResultMessage 连带驱逐 |
| 九段式结构化摘要 | Claude Code 九段 prompt（直接原型）+ AgentScope 四段（Java 实现先例） |
| Skill 按需加载（清单 + 正文按需） | Claude Code：skills 摘要常驻、正文按需，压缩后按 25K 预算重注入 invoked skills；CLAUDE.md `.claude/rules/` paths 按需加载；Anthropic progressive disclosure |
| 压缩后显式恢复 | Claude Code post-compact restore（5 文件/50K、skills/25K、plan、CLAUDE.md 重读）；AgentScope 摘要消息带 offload 路径引用 + `session_search` |
| 溢出兜底（撞硬限后自救） | AgentScope `recoverFromOverflow`（triggerMessages=1 强制压缩 + 重试一次）；Claude Code PTL 重试（按 API round 丢头部分组，≤3 次） |
| 认知可观测（Span+Event） | 本调研未深入；可另查 Langfuse / OpenTelemetry GenAI semantic conventions / AgentScope Studio |
