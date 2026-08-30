# 「做完美」第二轮 best-of-breed 核验（Tier-2/3）

> **来源**：effort #2 研究票 [T28](../../.wayfinder/tickets/T28-oss-perfect-tier23-verification.md)，4 个并行 research 子 agent（core / memory / spill / guard）web + GitHub API 核验，2026-08-14。
> **用途**：spec 12 的事实源；把第一轮 [oss-best-of-breed.md](oss-best-of-breed.md) 的 Tier-2/3 backlog 按 **「stars ≥ 10K 开源项目」硬门槛**重新核验并深挖实现细节。
> **口径**：采纳事实源只认 GitHub stars ≥ 10K 的 OSS 项目；非 OSS 标准/论文（MSRC FIDES、IETF AAT、Anthropic 文档、Claude Code 文档镜像）只作辅助注记。star 数为 GitHub REST API 当日精确值。
> **上游**：第一轮 Tier-1 已全部落地（docs/spec/11，wayfinder T16–T27 闭合）；本轮对象 = Tier-2 全量 + Tier-3 精选。

---

## 0. 一句话结论

- **core**：测试基建先行——FakeChatModel+record/replay（Vercel/Pydantic AI 均无官方录制，Buzhou 自建 JSON 脚本队列）是其余一切回归的地基；Run 注册表 + 事件溯源 + 幂等键（Mastra→Temporal 谱系）把悬空修复升级为 proactive 恢复；**重要修正**：LangGraph superstep 并非「任一失败→整批回滚」，事务性并行批须自设「批提交」语义。
- **memory**：最便宜的是 evictRatio 部分逐出（Letta 默认 0.3 摘要/0.7 保留 + 10% 步进）；sleep-time 后台整理与已落地的对账/双时序严丝合缝；memory-as-tools 必须带 provenance+taint 防投毒（超越 Unit 42 公开建议水位）。
- **spill**：head+tail 窗口风味（Codex 实测为**头尾各半掐中间**、v0.56 起转 token-based）+ context-clearing（Anthropic：清旧 tool_result 是最安全最轻的压缩）+ chunk hash 校验（git 惯例）三件小而高 ROI；语义回读是「locate（语义）→ fetch（byte）」两段式而非第五种并列语义；AST 切片的 JVM 绑定全部不达标（JavaParser 6.1K / tree-sitter-java 138），达标事实源 = aider+tree-sitter+langchain 主库、JVM 实现走工程注记。
- **guard**：promptfoo（24.2K，唯一达标红队）CI 门性价比全榜第一；FIDES taint 最小可行 =「只标 + 写门校验」（论文全文已取：join 半格传播、AgentDojo 注入成功数归零、效用损失 4.5–16.2%）；OPA 达标但**无成熟 JVM 内嵌**（opa-java 26 star 仅 REST 客户端）→ 内嵌自有可分析子集 + OPA sidecar SPI；E2B 实测 13.4K 达标（高于预期），沙箱三档（Deno 轻 / Firecracker 重 / E2B 托管）全达标。

---

## 1. Star 数核验总表（2026-08-14，GitHub REST API）

### 1.1 达标（≥10K，可作采纳事实源）

| repo | stars | 备注 |
|---|---|---|
| langchain-ai/langchain | 144,172 | Deep Agents 已并入主包 |
| anthropics/claude-code | 141,341 | ⚠️ 非 OSS（license 空，文档/issue 镜像）→ 仅注记 |
| denoland/deno | 108,248 | 沙箱轻量档 |
| openai/codex | 105,721 | head+tail 反面教材 + 风味源 |
| microsoft/autogen | 60,404 | CancelMode/CancellationToken |
| git/git | 62,540 | 内容寻址概念锚点 |
| mem0ai/mem0 | 63,201 | 对照 |
| cline/cline | 66,136 | 压缩前检查点达标源 |
| crewAIInc/crewAI | 57,038 | 对照 |
| run-llama/llama_index | 51,621 | 对照 |
| Aider-AI/aider | 48,169 | AST/repomap |
| langchain-ai/langgraph | 39,627 | superstep/interrupt/fork |
| firecracker-microvm/firecracker | 36,040 | 重载沙箱 |
| continuedev/continue | 35,474 | 对照 |
| modelcontextprotocol/servers | 89,534 | MCP 生态成熟度锚定（spec 仓本身 8,948 ✗） |
| getzep/graphiti | 29,889 | **实测达标**（第一轮预期 <10K，已过线） |
| openai/openai-agents-python | 28,616 | 取消/错误回喂 |
| mastra-ai/mastra | 27,179 | org 已从 mastra-inc 迁移 |
| vercel/ai | 26,168 | Mock 模型 |
| dapr/dapr | 26,021 | 事件溯源对照 |
| tree-sitter/tree-sitter | 26,632 | AST 主库（C） |
| letta-ai/letta | 24,230 | memory 工具/recall/sleep-time |
| promptfoo/promptfoo | 24,206 | **红队门唯一达标源** |
| temporalio/temporal | 22,284 | 事件溯源金标准 |
| google/adk-python | 21,094 | 对照 |
| microsoft/onnxruntime | 21,369 | 分类器达标承载源（Java API） |
| pydantic/pydantic-ai | 19,271 | TestModel/ModelRetry |
| 567-labs/instructor | 13,726 | org 已从 instructor-ai 迁移 |
| e2b-dev/E2B | 13,383 | **实测达标**（托管沙箱） |
| open-policy-agent/opa | 12,099 | policy-as-code 概念源（JVM 内嵌无门） |

### 1.2 不达标（裁决：注记 / 出界）

| repo | stars | 裁决 |
|---|---|---|
| spring-projects/spring-ai | 9,299 | 注记：官方无 FakeChatModel（仅 evaluation 模块），候选 2 事实源改用 vercel/ai + pydantic-ai |
| NVIDIA/garak | 8,792 | 换达标源 promptfoo，本仓注记 |
| modelcontextprotocol/modelcontextprotocol | 8,948 | 注记：structuredContent 规范文本；widget/UI 对 headless 库**出界** |
| guardrails-ai/guardrails | 7,281 | 注记：on_fail 动词汇 Tier-1 已萃取 |
| meta-llama/llama-models | 7,678 | 注记：模型本体在 HF（gated）；承载换 onnxruntime |
| NVIDIA-NeMo/Guardrails | 6,937 | 注记：概念萃取完成；其文档自曝 tool 参数/结果旁路缺陷 |
| javaparser/javaparser | 6,133 | 工程注记：Java 全 AST 首选（纯 JVM 无 native），非达标源 |
| microsoft/PyRIT | 4,291 | 换达标源 promptfoo |
| restatedev/restate | 4,287 | 注记：idempotency-key→稳定 invocation id 语义最贴 agent，同义已由 Temporal/Dapr 覆盖 |
| cedar-policy/cedar | 1,656 | 注记备选：cedar-java（75 star）是唯一 JVM 内嵌引擎（Maven `com.cedarpolicy:cedar-java:4.3.1`，JNI+五平台原生库） |
| protectai/rebuff | 1,519 | **出界**：已归档；canary 能力 Tier-1 已自研落地 |
| open-policy-agent/opa-java | 26 | 注记：OPA 官方 Java 客户端，仅 REST 调 sidecar，无内嵌 |
| tree-sitter/java-tree-sitter | 138 | 工程注记：官方 Java 绑定，native 运维成本 |
| microsoft/fides | 111 | 注记：FIDES 论文教程仓，语义以 arXiv 2505.23643 为准 |

---

## 2. core（运行时编排）

### 2.1 持久 Run 注册表 + 枚举续跑（Mastra 27,179★）
- **实现**：`WorkflowsStorage.listWorkflowRuns({status,fromDate,toDate,resourceId,page})`（running/waiting/suspended/failed/success）+ `getWorkflowRunById` + `persistWorkflowSnapshot`；快照=`WorkflowRunState`（runId/status/context/activePaths/suspendedPaths/result/error/timestamp）。`restart()`=从快照 rehydrate 后由最近活跃 step 重新驱动（**可能重跑已完成 step，恢复非幂等**）；`resume()`=从 suspended 点精确续跑（resumeData 过 step.resumeSchema 校验）。已知坑：issue #5549 服务器重启后 run 不自动恢复。
- **Buzhou 形状**：`RunRegistry`（listRuns/getRun/persistRunState，**以 Completed-Turn 为快照单元**）+ `RunHandle.restart()`=从最后 Completed-Turn 之后续跑；**与既有 lease 强绑**——restart 前必须先拿到该 run 租约，拿不到即拒绝（补上 Mastra 未明的并发防护）。
- **工作量**：3–5 天（InMemory+JDBC 两实现）。**ROI：高**。

### 2.2 FakeChatModel + record/replay（vercel/ai 26,168★、pydantic-ai 19,271★）
- **实现**：Vercel：`MockLanguageModelV4`+`mockValues`（按调用次序消费数组、耗尽重复末值）、`simulateReadableStream`（可配 chunk 延迟）；**官方无录制/回放**。Pydantic AI：`TestModel`（自动调全工具、参数由 schema 程序化生成）、`FunctionModel(fn(messages,info))` 手工构造 `ToolCallPart`、`capture_run_messages()` 捕获交替列表、`ALLOW_MODEL_REQUESTS=False` 全局防真实请求。Spring AI 9,299★ 不达标且无 fake（仅 evaluation）。
- **Buzhou 形状**：`FakeChatModel implements ChatModel/StreamingChatModel` 持脚本队列（按调用序弹出，脚本项可含**单条 assistant 消息多个 toolCall 的并行块**——并行回放关键语义）；`RecordingChatModel` 装饰真模型落 JSON；回放按请求序列匹配，**失配即测试失败**（防静默漏断言）。
- **工作量**：1–2 周。**ROI：高（其余候选的回归地基，应最先做）**。

### 2.3 显式取消 CancelMode + token 贯穿（autogen 60,404★、openai-agents-python 28,616★）
- **实现**：AutoGen：`CancellationToken` 贯穿 runtime→agent→model client→工具上下文，触发即 `CancelledError`，**未消费 partial 丢弃、TaskResult 不返回**；双词汇=立即 abort vs `ExternalTermination`（当前 turn 完成后停）；issue #4029 承认 agentchat 层有缺口。OpenAI：无专门 API，cancel 后**须继续消费 stream_events 完成清理**，部分结果可从 `new_items` 挽救。Pydantic AI 补充：`RunCancelled` 终态（run 正常返回且保留部分消息）。
- **Buzhou 形状**：`enum CancelMode { IMMEDIATE, AFTER_CURRENT_TOOLS, AFTER_CURRENT_TURN }`（AutoGen 两档+中间档；JDK21 下 IMMEDIATE=虚拟线程 interrupt、AFTER_CURRENT_TOOLS=等 StructuredTaskScope join）；token 贯穿 TurnLoop 与工具执行器（`InterruptibleTool` 可选接口让长任务主动检查）；落盘策略：AFTER_CURRENT_TURN 保留部分输出入 Completed-Turn、IMMEDIATE 丢弃在飞结果（防半成品泄漏）。
- **工作量**：约 1 周。**ROI：高**。

### 2.4 工具参数 schema 校验 + per-turn 重试预算（pydantic-ai 19,271★、instructor 13,726★）
- **实现**：Pydantic AI：工具参数 `ValidationError` **自动**转 retry 消息回喂（"the call didn't work, here is why, try again"）；`ModelRetry` 异常触发重试、普通 `ToolFailed` 中止 run——两档错误词汇分明；**默认 retries=1**，可 agent 级/per-tool/per-output 配置。Instructor：`max_retries`（示例 3，Tenacity 底座）、REASK=校验错误回传重生成、`llm_validator` 语义级校验。
- **Buzhou 形状**：工具执行**前**对 arguments 做 JSON Schema 校验（复用 spring-ai 工具 schema 生成）；失败构造 `ToolValidationFeedback`（区别于执行期 `ToolErrorFeedback`）；`TurnLoopPolicy` 增 `retryBudget`（per-turn 计数器，默认 1–2，与 Turn 上界独立扣减），耗尽转 REASK_FAILED 停止条件。
- **工作量**：2–3 天。**ROI：高**。

### 2.5 事件溯源工具调用日志 + 幂等键（temporal 22,284★、dapr 26,021★；restate 4,287 注记）
- **实现**：Temporal：Event History 唯一事实源；恢复=从头重放代码按 history 喂回已记录结果，Activity 结果只记一次、replay 复用不重算（LLM 须放 Activity 正因如此）；确定性约束（禁裸时间/随机/IO）；写型 Activity 应幂等，幂等键惯例由 WorkflowId(+activityId) 派生。Dapr：同款 event-sourced history+replay，重放主动检测非确定性抛错。Restate：`idempotency-key` header→稳定 invocation id，重试并入同一 invocation——语义最贴 agent 但不达标。
- **Buzhou 形状**：**不引入 workflow engine**，取两点：(a) 追加式 `ToolCallLog`（turnId、toolCallId、请求指纹、outcome），restart 时已落盘 outcome 的 toolCall 按 id 短路不重跑；(b) 幂等键 = sessionId+turnId+toolCallId 随调用传给工具端。**replay 不重跑 LLM**：以 Completed-Turn 为恢复点、恢复=最后 Completed-Turn 之后续跑，天然规避 Temporal 式全量重放（agent 场景比 workflow 更友好之处）。
- **工作量**：1–2 周。**ROI：中高（Run 注册表安全性的前提）**。

### 2.6 事务性并行批（langgraph 39,627★）——**语义重要修正**
- **修正**：LangGraph superstep **并非**「任一分支失败→整批回滚」。准确语义：node 对 channel 的写在 superstep 末统一 apply（同批互不可见，快照隔离）；失败 task 的写不提交、错误记为 ERROR pending write；**兄弟成功 task 的写保留为 `checkpoint_pending_writes`**；恢复时成功写重放、失败 task 重跑；**外部副作用无任何回滚**——「事务性」只存在于状态写入层。
- **Buzhou 形状**：并行工具批=「批提交」：全部成功才把整批 ToolResponse 追加 history 并落 Completed-Turn；任一失败→失败者走 `ToolErrorFeedback`、成功者结果暂存批记录，**策略显式可配**（全部回喂 / 仅失败回喂）而非隐式。
- **工作量**：2–4 天。**ROI：中（事件溯源打底后很便宜）**。

### 2.7 time-travel/fork + HITL interrupt/resume（langgraph 39,627★）
- **fork 实现**：`get_state_history()` 枚举 StateSnapshot 链（parent_config 逆序，metadata.source=loop|input|fork|update）；fork=取历史 config→`update_state(past_config, new_values)` 生成新 checkpoint→从该点续跑。
- **interrupt 实现**：`interrupt()` 暂停抛值；`Command(resume=...)` 恢复时**所在 node 从头重执行**（已知反模式：interrupt 前的副作用重跑）；resume 值按 node 内出现顺序匹配，多个 pending 必须 `{interrupt_id: value}` 映射否则 RuntimeError。
- **Buzhou 规避**：**以 toolCallId 匹配**（非节点内顺序）：Turn 挂起时记录 `pendingToolCalls[]`（toolCallId、args 指纹）；`resume(toolCallId, payload)` 精确注入对应 ToolResponse、可逐个 resume；**绝不重放 turn 前段**（未落盘 turn 丢弃重开，同批已执行工具结果按 2.6 暂存保留）——直接消除 LangGraph node 重执行反模式。fork：Completed-Turn 即 checkpoint，`Session.listCompletedTurns()` + `forkFrom(turnId)` 把截至该 turn 的 history 复制到新 sessionId 续跑（Buzhou state=消息列表，无需 channel 版本机制）。
- **工作量**：约 1 周（7+8 捆绑，共享检查点设施）。**ROI：高**。

### 2.8 实施顺序建议
**2（测试地基）→ 4 → 3 → 1+5（绑定做）→ 7+8 → 6**。

---

## 3. memory（渐进式压缩）

### 3.1 memory-as-tools + 防投毒（letta 24,230★；Unit 42 注记）
- **实现**：`core_memory_replace(old,new)`：精确子串匹配，未命中抛 ValueError，**多处命中抛唯一性错误**——出现次数检查即防静默覆写；只读 block 一律拒绝。`core_memory_append`（`\n` 拼接）、`memory_rethink`（整块重写）、`memory_insert(line)`、`archival_memory_insert/search(query,tags,top_k)`。block 默认上限 persona/human 20,000 字符（新体系每 block ~2,000）。
- **投毒事实**（Unit 42，非 OSS 注记）：PoC 打**会话摘要 LLM 调用**——工具输出（网页）是摘要 prompt 唯一攻击者可控槽位；伪造 `</conversation>` 标签使指令逃逸对话块、伪装模板级指令，折叠进摘要后随主题写入持久记忆（可存留 365 天）后续激活。Unit 42 缓解=输入预处理/Guardrails/URL 白名单/记忆交互日志，**未提 taint/写确认**；Letta 自身缓解仅到只读 block+精确匹配报错。
- **Buzhou 形状**：暴露 `revise_summary_section(section_id, old_text, new_text)`——沿用「精确匹配+唯一性检查+类型化错误 `EDIT_NOT_FOUND`/`EDIT_AMBIGUOUS`」；P0 段只读锁；每次写入带 **provenance（来源 message id）+ taint 位**（evidence 源自工具输出的内容标 untrusted，未经脱敏不得进摘要正文、只进 scope 受限 evidence 区）；写操作全量审计日志。**防投毒水位超越 Unit 42 公开建议**。
- **工作量**：3–5 天。**ROI：高**。

### 3.2 向量 recall 三模搜（letta 24,230★）
- **实现**：`conversation_search` 四模式——text（Turbopuffer BM25；降级 PG ILIKE）、embedding（query 向量化 ANN）、timestamp（created_at 范围倒序）、hybrid（RRF 融合，vector_weight/fts_weight 可调）；双写 SQL（事实源）+向量库；分页=SQL `sequence_id` 游标（after/before+limit）、向量侧 top_k+时间过滤；返回精确原文+元数据；**过滤工具消息与检索自身消息防递归自指**。
- **Buzhou 形状**：`RecallSearchQuery{mode: TEXT|EMBEDDING|TIME|HYBRID, query, start/end, after/before sequenceId, limit}` 落在既有消息台账上加 pgvector 列 + HNSW（**单库双写、事务内同写避免漂移，不引入独立向量库**）；与 `EvidenceLookupTool` 互补——确定性指针管精确回读、三模搜管模糊召回。
- **工作量**：5–8 天。**ROI：中高**。

### 3.3 episodic memory few-shot（langgraph 39,627★ / langchain 144,172★）
- **事实**：LangChain 官方三类长期记忆 semantic/episodic/procedural；LangMem 明确「episodic 将成功交互保存为学习示例」。实现面=BaseStore 层级命名空间+可选向量 IndexConfig，`put` 存轨迹、`search` 语义召回、动态 prompt 函数注入 system prompt 作 few-shot。**「成功工具序列采集→注入」是文档层模式，LangGraph 本体只提供 Store 原语**。
- **Buzhou 形状**：`EpisodeLedger{task_signature, goal, tool_trace_digest, outcome, embedding}`；采集 hook 在任务成功判定后（或 sleep-time 蒸馏）写入；新任务以 goal 向量召回 top-k 按预算渲染进 system prompt「过往成功示例」块。
- **工作量**：约 5 天。**ROI：中（依赖成功信号判定质量，排后）**。

### 3.4 sleep-time 后台整理（letta 24,230★）
- **实现**：`sleeptime_agent_frequency` 计 turn 触发；与主 agent **共享同一 memory block 数据库实体**（attach_block_async 同步挂载）；整理动作=memory_replace/insert/rethink（去冗余、重排、写 archival）；热路径隔离=turn 结束后创建后台 Run（RunStatus.created）、safe_create_task 异步执行绝不阻塞主响应。
- **Buzhou 形状**：turn 后 hook 投递 `MemoryConsolidationTask` 到专用 executor（**JDK21 虚拟线程 + 每 session 串行化避免 block 写竞争**）；整理器跑 SummaryFactReconciler 对账、去冗余、P0–P3 重排、archival 归档——全走双时序台账；失败退避重试、可开关、全程审计。
- **工作量**：4–6 天。**ROI：高（把对账挪出热路径，与已落地组件严丝合缝）**。

### 3.5 evictRatio 部分逐出保连续（letta 24,230★；cline 66,136★ 旁证）
- **事实**：Letta 博客「Generally, you should evict only a portion (e.g., 70%) of messages」；SDK compaction `sliding_window_percentage` **默认 0.3（摘要约 30%、保留约 70%）**、不够则按 ~10% 步进升级、`clip_chars` 摘要上限 50,000。旁证：Cline `COMPACTION_TRIGGER_RATIO=0.9`/`DEFAULT_TARGET_RATIO=0.7`；LangChain 保 10% 近期。
- **Buzhou 形状**：`evictRatio` 参数化（默认 0.7）+ 10% 步进梯子；不变式：最近 N turn 原文 + 上一次增量摘要永不逐出（与 summarized_message_ids 双水位兼容）。
- **工作量**：1–2 天。**ROI：高（极小改动防上下文断裂）**。

### 3.6 压缩前检查点可回滚（cline 66,136★）
- **实现**：auto-compact 在估算输入达安全上限 90% 触发；两策略 basic（确定性零 LLM：裁剪工具输出换 `<SYSTEM_NOTICE>`、保近期回复、剥过期附件）/agentic（二级 summarizer 找 cut point+生成 next steps 注记）；压缩失败强制 basic 兜底。检查点与压缩解耦：**每次工具使用后** git 影子仓快照；回滚三档=Restore Files / Restore Task Only（删检查点后消息）/ 两者。
- **Buzhou 形状**：`CompactionCheckpoint{sessionId, seq, preWatermark, messageWindowRef}`——compact_now/增量摘要提交前把压缩前消息窗不可变快照按水位键存；回滚三档：仅恢复消息窗 / 恢复消息窗并撤销摘要生效（双时序 valid_to 直接表达）/ 连同事实台账回滚（默认关）。
- **工作量**：3–4 天。**ROI：中高**。

### 3.7 压缩保真度 eval（langchain 144,172★）
- **事实**：Deep Agents 默认 85% 触发、保 10% 近期；eval 方法论=自有 traces「向应压缩与不应压缩的线程注入 follow-up prompts」构造正负用例验证触发时机（terminal-bench-2 零误触发）+ dogfooding。**诚实声明**：「断言 follow-up 答案压缩后仍可答」的保真断言模式是子 agent 基于 trace 注入框架的合理延伸，标注「建议自建」。
- **Buzhou 形状**：`CompactionFidelityEval`：回放录制会话→注入仅在压缩前水位之下可答的 follow-up→用压缩后上下文跑 agent→LLM judge + **evidence-id 精确断言**答案保住；负例集（不应压缩场景）；指标=保真率/误触发率/压缩比；prompt 变更 CI 门禁。
- **工作量**：5–8 天。**ROI：高（九段式摘要的质量护栏）**。

### 3.8 实施顺序建议
**5（evictRatio，1–2 日）→ 4（sleep-time）→ 1（memory-as-tools+taint）→ 7（保真 eval）→ 6（检查点）→ 2（三模搜）→ 3（episodic）**。

---

## 4. spill（溢出保护 / 大输出 offload / 回读）

### 4.1 head+tail 窗口回读风味（openai/codex 105,721★，反面教材+风味源）
- **事实（修正）**：v0.24.0（2025-08）Bash 输出 256 行或 10KiB **先到先截，策略=头 128 行+尾 128 行掐中间**；v0.56 扩展到 MCP 工具输出；社区批评（#6426/#5913）：行数与 token 无关、掐中间破坏结构化数据；后续新增 `tool_output_token_limit`（config.toml）转 token-based；无省略量标记。
- **Buzhou 形状**：`ReadRangeTool` 的 `mode=byte` 增 `window=head|tail|head_tail` 风味参数（headBytes/tailBytes 默认对称）；中段以**显式标记行**替代（`…[omitted N bytes, offset X..Y; refetch via mode=byte]`）。与 Codex 本质差异：销毁式截断 vs 原始字节在 spill 存储完整保留可无损回取。
- **工作量**：小。**ROI：高（最便宜增量）**。

### 4.2 JIT 标识纪律 + context-clearing（Anthropic 文档/Claude API，非 OSS 注记）
- **事实**：清除深层历史已消费 tool_result 是「safest lightest touch forms of compaction」（原始结果对后续推理几乎无信号）；JIT=只持轻量标识（路径/查询/链接），元数据本身是导航信号。Claude API 形状：`context_management.edits[] = {type:"clear_tool_uses_20250919", trigger:{input_tokens:N}, keep:{…}}`，只清 tool_result 内容保消息结构；**cache 代价**：清除改写 prompt 会 cache miss，断点放置重要。
- **Buzhou 形状**：自实现 ConversationPostProcessor：上下文超阈值时把旧 tool_result 替换为 Handle 占位（"cleared; refetch via ReadRangeTool(evidence-id)"）保最近 N 个完整；显式逐出=`EvictHandleTool`/引用计数 TTL。**跨 provider 由 harness 自持（Claude API 是 server 侧、仅 Anthropic；Buzhou 版本对所有模型生效）**。
- **工作量**：中。**ROI：高（Handle 价值闭环的关键）**。

### 4.3 内容寻址 chunk hash 回读校验（git/git 62,540★ 概念锚点）
- **事实**：git 对象名即内容 hash、读回重算必校验（whole-object CA）；切片级需自建摘要表（类 Merkle：叶=chunk、root 进 evidence-id）。
- **Buzhou 形状**：spill 落盘时除 whole-content hash 外记录 chunk 摘要（每切片 sha256，可选 Merkle root）；回读响应 envelope 附 `{data, byteRange, chunkSha256, handleRoot}`；校验失败走既有读侧 lenient（warning）/写侧 strict 非对称。
- **工作量**：小–中。**ROI：中高（腐化/TOCTOU 可检测，evidence-id 闭环）**。

### 4.4 语义回读第 4 模式（letta 24,230★）
- **事实**：archival=通用向量 DB（pgvector），插入时切块、`archival_memory_search(query, tags, page)`；无相似度阈值参数；片段不能 pin 上下文、必须工具按需查询——与 Buzhou Handle 哲学同构。OpenAI File Search（非 OSS 注记）同向。
- **Buzhou 形状**：durable/cold 层 offload 时**异步**按既有切片边界 embed（hot-tail 不索引，与两级保留对齐）；`ReadRangeTool` 增 `mode=semantic`（query, k, minScore 可选, tag/filter）返回 top-k chunk 条目（evidence-id+byte offset+摘要），模型再以 `mode=byte` 精读——**语义是「定位」、byte/jsonpath/pagination 是「取回」，两段式组合而非并列第五种语义**。默认关。
- **工作量**：中偏大（embedding provider 抽象+向量存储+异步索引）。**ROI：中高**。

### 4.5 AST-aware 切片边界（aider 48,169★ / tree-sitter 26,632★ / langchain 144,172★；JVM 绑定全不达标）
- **事实**：Aider repomap=tree-sitter tags `.scm` 抽 def/ref→MultiDiGraph（边权 mul*sqrt(refs)、chat 提及 x50）→个性化 PageRank→**token 预算二分**（±15% 提前接受，map_tokens=1024）→TreeContext 只渲染 lines-of-interest、每行截 100 字符；解析失败跳过或 Pygments 兜底。「先切再解析避 32KB cliff」=不把整文件/残片喂解析器，先按安全边界切段、段内解析、超长行硬截。LangChain：`RecursiveCharacterTextSplitter.from_language(Language.JAVA)` 用递归声明分隔符（`"\npublic "` 等）；AST 版 splitter 仅 experimental。
- **JVM 侧（工程注记，非达标源）**：Java 全 AST 用 **JavaParser**（6,133★ 纯 JVM 无 native，优先）或 tree-sitter/java-tree-sitter（138★，io.github.tree-sitter Maven 组，多语言但 native 运维成本）；其他语言走 LangChain 式分隔符表+行边界启发式回退，不追求全语言 AST。
- **工作量**：中。**ROI：中（可后置）**。

### 4.6 结构化 offload（structuredContent + 下载 URL + poll_token）——**出界裁决**
- **事实（修正）**：MCP spec 仓 8,948★ 不达标；`structuredContent`+`outputSchema`+`resource_link`+`annotations.audience` 双受众是规范；**poll_token 非 MCP 标准**（来自 futuresearch 博客模式：preview+csv_url+poll_token 换一次性短时效 download token）；spec 原生异步是 Tasks（SEP-1686）。
- **裁决**：Buzhou 为 headless Java 库，widget/iframe 渲染**出界**；一次性 download token 属 HTTP 传输层（Buzhou 进程内无此边界）降级注记（未来做 MCP server facade 再评估）；`structuredContent` 的「结构化字段+兼容文本序列化」双字段形状可作 Handle 输出格式借鉴注记（可给 Handle 加 outputSchema 描述）。

### 4.7 实施顺序建议
**1 head+tail（小/高）→ 2 context clearing（中/高）→ 3 chunk hash（小中/中高）→ 4 semantic fetch（中大/中高）→ 5 AST 边界（中/中）**。

---

## 5. guard（Hook 护栏 / HITL / 防注入）

### 5.1 CI 自动红队门（promptfoo 24,206★，红队唯一达标源）
- **实现**：`promptfooconfig.yaml` 的 `redteam:` 段配 plugins（prompt-injection/pii/excessive-agency/tool-discovery/shell-injection/sql-injection）与 strategies（jailbreak:meta 单轮、jailbreak:hydra 多轮自适应）；target 为任意 HTTP/OpenAI 兼容端点；`promptfoo redteam run` + `--fail-on-error` 控门禁；官方 GitHub Action；攻击/评分模型可换本地 Ollama 全离线。
- **Buzhou 形状**：harness 暴露测试用 HTTP target；仓库内 `redteam/promptfooconfig.yaml`；**nightly 流水线跑、先不阻塞 PR（观测期）**。
- **工作量**：2–3 天。**ROI：高（性价比全榜第一）**。

### 5.2 FIDES 式 taint 信息流控制（arXiv 2505.23643，MSR 论文注记源，高价值）
- **语义**（全文已取）：标签=join 半格（机密性 {L,H} 或读者集合 join=交集；完整性 {T,U} 或写者集合 join=并集；取积格）。传播点：①工具结果在 JSON 树节点级挂标签；②LLM 响应保守取全部输入标签的 join；③会话历史维护累积标签；④工具执行前检查 `policy(action)`、结果标签=读集⊔工具标签⊔实参标签。逃生舱：`query_llm`（隔离 LLM+约束解码，输出乘类型容量格 bool⊑enum⊑string）与变量隐藏 Hide/Expand。保证=explicit secrecy（非 non-interference）。**AgentDojo：策略开启后注入成功数 0；效用损失 4.5–16.2%**。
- **Buzhou 形状**：读侧 hook 给工具/RAG 输出 Attachment 打 `TaintLabel`（枚举起步：TRUSTED/UNTRUSTED+来源）；LLM 响应 join 传播进会话状态；写门（物理阻断+HITL 工具调用前）校验「上下文标签⊔实参标签」——失败走既有 session-state 授权+TTL（=FIDES approver 的 Buzhou 等价物）。**最小可行 taint=只标+写门校验**（不做变量隐藏/隔离 LLM）。
- **工作量**：MVP 4–6 天；二期（Hide/Expand+类型容量格）+5–8 天。**ROI：高（复用既有 hook→state→Attachment 链，读写非对称的形式化升级）**。

### 5.3 ECDSA 签名审计链（IETF AAT 草案，注记源）
- **事实**：draft-sharif-agent-audit-trail-00（2026-03、无 WG）；必填 11 字段（record_id/timestamp/agent_id/agent_version/session_id/action_type/action_detail/outcome/trust_level/parent_record_id/prev_hash）；链语义 `prev_hash(N)=SHA-256(JCS(record(N-1)))`（**RFC 8785 JCS 强制**）；签名可选 ECDSA P-256：去 signature 字段 JCS 序列化→SHA-256→签名→Base64url **IEEE P1363 r||s 64 字节**（非 JWS DER）；session 收尾 session_hash。
- **Java 可行性**：JDK 内置 `SHA256withECDSA`，但输出 DER 需自转 P1363（约 30 行）；主要工作量是 JCS 规范化（JDK 无内置，小型第三方或自写约 200 行）。
- **工作量**：3–5 天。**ROI：中高（纯本地、给确定性事实采集加防篡改）**。

### 5.4 run_command 硬隔离沙箱三档（firecracker 36,040★ / deno 108,248★ / E2B 13,383★ 全达标）
- **事实**：**Firecracker jailer**：`--id/--exec-file/--uid/--gid/--cgroup/--chroot-base-dir/--netns/--resource-limit/--daemonize/--new-pid-ns`；隔离=关继承 fd+清环境、复制二进制进 chroot、setrlimit、mount ns+pivot_root、mknod /dev/kvm 与 /dev/net/tun、降权 exec；**需 root、Linux-only**（KVM/cgroups）、musl 静态二进制；启动延迟约 125ms（README 宣称）。**Deno**：deny-by-default，`--allow-read=<路径>/--allow-net=<host:port>/--allow-env=<变量名>/--allow-run=<程序名>`，`--no-prompt` 未授权即抛错，跨平台。**E2B**：Firecracker 云沙箱，JS/Python SDK+OpenAPI REST（ApiKeyAuth）；出网管控 allowOut/denyOut（CIDR/域名）、per-domain header 注入；可自托管。
- **Buzhou 形状**：`CommandSandbox` SPI 三实现，全 optional 探测+Linux gating（os.name+/dev/kvm 探测）：①`DenoSandbox`（轻量档跨平台，开发环境默认指引）②`FirecrackerSandbox`（重载档生产 Linux，jailer 全参暴露）③`E2BSandbox`（REST 客户端，无本地 KVM 托管场景）。
- **工作量**：Deno 档 3–4 天；Firecracker 档 7–10 天（rootfs 准备是大头）；E2B 档 4–5 天。**ROI：Deno 高、Firecracker 中、E2B 视部署形态**。

### 5.5 policy-as-code 授权引擎（opa 12,099★ 达标；cedar 系不达标）
- **事实**：OPA 达标但 JVM 现实=「无成熟内嵌 Rego」——官方 opa-java（26★）明言"calls a running OPA server over its REST API — does not embed"，提供 OPAClient.check/evaluate（Maven io.github.open-policy-agent:opa）。cedar-java（75★）是唯一 JVM 内嵌成熟路径（Maven com.cedarpolicy:cedar-java:4.3.1，JNI 绑 Rust、五平台原生库、Java 8 兼容）但双不达标。
- **Buzhou 形状**：内嵌「**自有可分析子集**」（声明式 JSON 规则：主体×工具×资源×label 谓词→allow/deny/escalate），语义对齐 OPA「结构化 input→决策+reason」模型（OPA 为达标概念源）；留 `PolicyEngine` SPI + **OPA sidecar adapter**（opa-java optional 依赖）给要 Rego 全表达力的部署方；cedar-java 注记备选。
- **工作量**：内嵌子集+SPI 3–5 天；OPA adapter 2 天。**ROI：高（与既有危险工具门同构，是其泛化）**。

### 5.6 分层分类器栈（onnxruntime 21,369★ 达标承载源；模型本体 HF gated 注记）
- **事实**：模型不在 GitHub 生态（llama-models 7,678★ 不达标）；Prompt Guard 2（22M/86M）检测注入+越狱、社区广泛 ONNX 化；Llama-Guard-3-8B 为 8B 后置审核；WARD 等评测显示此类分类器可被绕过（只作纵深一层）。
- **Buzhou 形状**：`InjectionClassifier` 接口+默认 `OnnxPromptGuard`（onnxruntime Java optional 依赖+模型文件探测式加载，gated license 用户自备）；8B 后置分类器委托外部推理服务（OpenAI 兼容端点）；**默认关**（Buzhou 已有确定性 spotlighting+canary，分类器是概率层）。
- **工作量**：ONNX 86M 路径 3–4 天。**ROI：中**。

### 5.7 NeMo Guardrails（6,937★，注记）
- Colang 语义：input rails→retrieval rails（RAG chunk 过滤/改写后才进 prompt）→execution rails（BotToolCalls 拦截改参/阻断）→output rails。**其文档自曝：tool 调用参数不经 input/output rails、tool 结果绕过 input rails**——恰是 Buzhou hook 链已覆盖的缺口。可萃取：检索上下文消毒 rail ≙ Attachment 读侧管道补 RAG 专属消毒 stage。**不建议引入依赖**。

### 5.8 实施顺序建议
**promptfoo CI 门 → FIDES 最小 taint → ECDSA 审计链 → Deno 轻量沙箱档 → 内嵌策略子集+OPA adapter → ONNX Prompt-Guard（默认关）→ Firecracker/E2B 重载档（按部署需求）**。

---

## 6. 汇总：采纳 backlog（喂 spec 12 的执行清单）

| 机制 | Tier-2 | Tier-3 / 精选 |
|---|---|---|
| **core** | FakeChatModel+record/replay[Vercel+Pydantic AI]；参数 schema 校验+重试预算[Pydantic AI+Instructor]；CancelMode 三档[AutoGen+OAI]；**Run 注册表+枚举续跑**[Mastra+lease] | 事件溯源 ToolCallLog+幂等键[Temporal+Dapr]；interrupt/resume 按 toolCallId+time-travel fork[LangGraph 规避反模式]；事务性并行批=批提交语义[LangGraph 修正版] |
| **memory** | sleep-time 后台整理[Letta]；memory-as-tools+provenance/taint 防投毒[Letta+Unit 42 超越]；向量 recall 三模搜（pgvector 单库）[Letta]；episodic few-shot[LangGraph] | evictRatio 0.7+步进梯子[Letta]；压缩前检查点三档回滚[Cline]；压缩保真 eval（evidence-id 断言）[LangChain 延伸] |
| **spill** | head+tail 窗口风味+显式中段标记[Codex 反面→风味]；AST-aware 切片（JavaParser+分隔符回退）[aider/tree-sitter/langchain] | context-clearing+显式逐出[Anthropic→harness 自持]；chunk hash 回读校验[git 惯例]；语义回读两段式 locate→fetch[Letta]（durable-only 默认关） |
| **guard** | CommandSandbox 三档（Deno/Firecracker/E2B）；policy-as-code 内嵌子集+OPA sidecar SPI；ONNX 分类器（默认关） | **FIDES 最小 taint（标+写门）**[MSRC]；ECDSA 审计链[IETF AAT+JCS]；CI 红队门[promptfoo] |

### 6.1 出界清单（本轮不做，研究裁决）
- MCP widget/structuredContent UI 双受众渲染 + poll_token 下载端点（headless 无 UI 面）。
- Rebuff 依赖（已归档）；NeMo/Guardrails AI/PyRIT/garak 依赖（均 <10K，概念已注记萃取）。
- Temporal/Dapr engine 整体引入（只取事件溯源+幂等键思想）。
- FIDES 二期（变量隐藏/隔离 LLM/类型容量格）——fog，待 MVP 落地后评估。

---

## 7. 相对第一轮研究的关键修正

1. **LangGraph superstep ≠ 整批回滚**：实为 pending-writes 半事务（兄弟成功写保留、失败者重跑）；Buzhou 事务性并行批改为显式「批提交」策略。
2. **Codex 截断 = 头尾各半掐中间**（128+128 行），非保头截尾；且 v0.56+ 已转 token-based——印证 Buzhou token-aware 阈值方向正确。
3. **MCP poll_token 非 MCP 标准**（futuresearch 博客模式）；spec 仓 8,948★ 不达标 → widget 出界。
4. **graphiti 29,889★ 实测达标**（第一轮预期 <10K）；E2B 13,383★、promptfoo 24,206★ 亦达标（高于预期）。
5. **OPA 无成熟 JVM 内嵌**（opa-java 仅 REST 客户端）→ 选型改为内嵌自有子集+OPA sidecar SPI。
6. **Mastra org 迁移**（mastra-inc→mastra-ai）、**instructor org 迁移**（instructor-ai→567-labs）——star 数按新址核验。
7. **Letta `core_memory_replace` 的唯一性检查**（多处命中报错）是防静默覆写关键细节，第一轮未记录。
8. **Spring AI 9,299★ 不达标**（差 701）——官方亦无 FakeChatModel，测试基建须自建。

---

## 8. Sources（精选；子 agent 全量过程见 T28）

- **star 核验**：api.github.com/repos/{...}（2026-08-14）
- **core**：mastra.ai/docs/workflows/overview · github.com/mastra-ai/mastra/issues/5549 · ai-sdk.dev/docs/ai-sdk-core/testing · pydantic.dev/docs/ai/guides/testing/ · pydantic.dev/docs/ai/core-concepts/retries/ · python.useinstructor.com · DeepWiki microsoft/autogen · openai.github.io/openai-agents-python/running_agents/ · docs.temporal.io/workflows · docs.dapr.io/developing-applications/building-blocks/workflow/workflow-features-concepts/ · docs.restate.dev/services/invocation/http · DeepWiki langchain-ai/langgraph
- **memory**：DeepWiki letta-ai/letta · docs.letta.com/v1-sdk/messages/compaction · letta.com/blog/agent-memory/ · DeepWiki cline/cline · docs.langchain.com/oss/python/concepts/memory · langchain-ai.github.io/langmem/concepts/conceptual_guide/ · langchain.com/blog/autonomous-context-compression · unit42.paloaltonetworks.com/indirect-prompt-injection-poisons-ai-long-term-memory/
- **spill**：github.com/Aider-AI/aider（repomap.py）· docs.langchain.com/oss/python/integrations/splitters/code_splitter · github.com/openai/codex/issues/6426 与 /5913 · modelcontextprotocol.io/specification/2025-06-18/server/tools · futuresearch.ai/blog/mcp-results-widget/ · modelcontextprotocol.io/seps/1686-tasks · anthropic.com/engineering/effective-context-engineering-for-ai-agents · platform.claude.com/docs/en/build-with-claude/context-editing · docs.letta.com/guides/agents/archival-memory
- **guard**：github.com/firecracker-microvm/firecracker/blob/main/docs/jailer.md · DeepWiki denoland/deno · DeepWiki e2b-dev/E2B · DeepWiki promptfoo/promptfoo · arxiv.org/abs/2505.23643（FIDES 全文 v1） · datatracker.ietf.org/doc/draft-sharif-agent-audit-trail/ · huggingface.co/meta-llama/Llama-Prompt-Guard-2-86M · github.com/open-policy-agent/opa 与 /opa-java · github.com/cedar-policy/cedar-java · DeepWiki NVIDIA-NeMo/Guardrails
