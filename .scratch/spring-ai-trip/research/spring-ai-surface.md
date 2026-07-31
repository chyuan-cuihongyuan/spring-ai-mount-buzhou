# Spring AI API 表面事实调研（Harness 挂接点依据）

- 调研日期：2026-07-31
- 版本基线：**Spring AI 2.0.0**（GA，发布于 2026-06-12；对照最新 1.x 补丁版 1.1.8，同日发布）
- 来源标注：官方文档站 docs.spring.io（2.0.0）、Javadoc 2.0.0、GitHub 源码（spring-projects/spring-ai，文件内容读自 master 分支 = 2.0.1-SNAPSHOT，与 v2.0.0 标签路径一致；凡 `@since` 标注均以 Javadoc 为准）

---

## 1. 版本、坐标与基线要求

**事实**

- 最新稳定版：**2.0.0**（2026-06-12 发布于 Maven Central）。1.x 线最新为 1.1.8。文档站 Stable 线：2.0.0 / 1.1.8 / 1.0.9；Snapshot 线：2.0.1-SNAPSHOT。
  - 来源：https://central.sonatype.com/artifact/org.springframework.ai/spring-ai-bom （版本列表 API 确认 2.0.0 GA 及 1.1.x 全系补丁版）
- BOM 坐标：`org.springframework.ai:spring-ai-bom:2.0.0`（纯 BOM，不含 Spring Boot 管理，需另配 `spring-boot-dependencies` 或 Boot parent）。正式版全部在 Maven Central，无需额外仓库。
  - 来源：https://docs.spring.io/spring-ai/reference/getting-started.html
- 常用 starter 命名（2.0 模块化重组后）：`spring-ai-starter-model-openai`、`spring-ai-starter-model-anthropic`、`spring-ai-starter-model-chat-memory`、`spring-ai-starter-mcp-client`、`spring-ai-starter-mcp-client-webflux`、`spring-ai-starter-tool-search-advisor` 等。核心库模块：`spring-ai-client-chat`（ChatClient+Advisor）、`spring-ai-model`（模型抽象+工具调用+记忆）、`spring-ai-commons`。
  - 来源：https://github.com/spring-projects/spring-ai/blob/v2.0.0/pom.xml（modules 列表）
- 对 JDK 的要求：**JDK 17 为最低版本**（父 POM `java.version=17`，编译 JDK 区间 `[17.0.19,)`）。虚拟线程特性（JDK 21）可放心使用，17 只是地板。
  - 来源：https://github.com/spring-projects/spring-ai/blob/v2.0.0/pom.xml
- 对 Spring Boot 的要求：**Spring AI 2.0.x 支持 Spring Boot 4.0.x 与 4.1.x**（官方 Getting Started 原文）；master 分支构建基线为 Spring Boot 4.1.1-SNAPSHOT / Spring Framework 7。注意：1.1.x 才对应 Spring Boot 3.x，2.0 必须 Boot 4。
  - 来源：https://docs.spring.io/spring-ai/reference/getting-started.html
- MCP Java SDK 版本：`io.modelcontextprotocol.sdk:mcp-bom:2.0.0`（父 POM dependencyManagement 导入）。
  - 来源：https://github.com/spring-projects/spring-ai/blob/v2.0.0/pom.xml

**Harness 提示**：项目基线定为 JDK 21 + Spring Boot 4.0.x + Spring AI 2.0.0 即可；2.0 相对 1.x 的破坏性变更全部集中在升级笔记（下文大量引用），设计挂接点必须以 2.0 API 为准。

---

## 2. Advisor 链：接口现状与工具调用循环归属

**接口现状（包 `org.springframework.ai.chat.client.advisor.api`，模块 `spring-ai-client-chat`）**

- `Advisor`：父接口，`extends Ordered`，仅 `getName()`。常量 `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER = HIGHEST_PRECEDENCE + 200`（2.0.0 从 +1000 改为 +200）。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/api/Advisor.java
- `CallAdvisor`：`ChatClientResponse adviseCall(ChatClientRequest, CallAdvisorChain)`。
- `StreamAdvisor`：`Flux<ChatClientResponse> adviseStream(ChatClientRequest, StreamAdvisorChain)`。
- `BaseAdvisor extends CallAdvisor, StreamAdvisor`（since 1.0.0）：模板方法 `before(...)`/`after(...)`，自带 `DEFAULT_SCHEDULER`。
  - Javadoc：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/api/BaseAdvisor.html
- `CallAdvisorChain`：`nextCall(req)`、`getCallAdvisors()`、**`copy(CallAdvisor after)`**（创建只含指定 advisor 之后所有 advisor 的子链——递归 advisor 的关键设施）。
  - Javadoc：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/api/CallAdvisorChain.html
- 2.0.0 新增标记接口：`ToolAdvisor`（工具调用生命周期所有者，实现类：`ToolCallingAdvisor`、`ToolCallAdvisor`、`ToolSearchToolCallingAdvisor`）与 `MemoryAdvisor`（`BaseChatMemoryAdvisor` 实现之）。
  - Javadoc：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/api/ToolAdvisor.html
  - 来源：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"New: ToolAdvisor Marker Interface" / "New: MemoryAdvisor Marker Interface"）

**工具调用循环在链内还是链外？——2.0.0 起：链内。**

- `ToolCallingAdvisor`（since 2.0.0，默认 order = `HIGHEST_PRECEDENCE + 300`）是**递归 advisor**：内部 `do { callAdvisorChain.copy(this).nextCall(req); toolCallingManager.executeToolCalls(prompt, response); } while (isToolCall)`，把"思考→工具调用→再思考"整个循环搬进 Advisor 链；链上排在它后面的 advisor（order 更大者）**每次迭代都会被调用**，可拦截每一轮模型请求/响应（含 ToolResponseMessage）。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.java
  - 官方文档（Recursive Advisors 专页）：https://docs.spring.io/spring-ai/reference/api/advisors-recursive.html
- 流式路径：`adviseStream` 经 `ChatClientMessageAggregator` 聚合后递归调用 `internalStream`，工具执行切到 `Schedulers.boundedElastic()`；工具调用轮的 chunk 默认被过滤不下发（`.filter(ccr -> !isToolCallResponse(...))`）。
- **2.0.0 破坏性变更**：`internalToolExecutionEnabled` 从 `ToolCallingChatOptions` 及所有厂商 Options 中**移除**；所有 ChatModel 实现内的模型内部工具执行被**删除**（如 `AnthropicChatModel.Builder.toolCallingManager(...)` 已标记 `@Deprecated(since="2.0.0", forRemoval=true)`——"internal tool execution in AnthropicChatModel is superseded by ToolCallingAdvisor used via ChatClient"）。`ToolExecutionEligibilityPredicate` 亦移除，代之以 `ToolExecutionEligibilityChecker`（`Function<ChatResponse,Boolean>`，默认 `response != null && response.hasToolCalls()`），可注入厂商特定停止逻辑。
  - 来源：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"Removed: internalToolExecutionEnabled…"、"New: ToolExecutionEligibilityChecker in ToolCallingAdvisor"）
- **自动注册**：`DefaultChatClient.buildAdvisorChain()` 在每次 `call()/stream()` 时调用 `autoRegisterToolCallingAdvisor()`——除非 `AdvisorParams.toolCallingAdvisorAutoRegister(false)`（context key `TOOL_CALLING_ADVISOR_AUTO_REGISTER`）或链中已存在任何 `ToolAdvisor` 实现，否则**总是**追加一个 `ToolCallingAdvisor`（即使静态未配工具，为运行时动态注入的工具兜底）。链中最多允许一个 `ToolAdvisor`（`validateSingleToolAdvisor` 否则抛 `IllegalStateException`）。全局开关：`spring.ai.chat.client.tool-calling.enabled=false`。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/DefaultChatClient.java
  - 升级笔记：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"Automatic ToolCallingAdvisor Registration"）
- 自定义 `ToolCallingAdvisor` 构造参数的注入通道：① `ChatClient.builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention, toolCallingAdvisorBuilder)` 五参重载；② Spring Boot 中声明 `ToolCallingAdvisor.Builder<?>` Bean（`@ConditionalOnMissingBean` 可整体替换）；③ 属性 `spring.ai.chat.client.tool-calling.advisor-order`。
  - 升级笔记：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"New: ChatClient.builder() Overload…"）
- 链尾：`ChatModelCallAdvisor` / `ChatModelStreamAdvisor`（order = `Ordered.LOWEST_PRECEDENCE`）由 `DefaultChatClient` 自动追加在链底，真正调用 `chatModel.call(prompt)`。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/ChatModelCallAdvisor.java
- 链实现：`DefaultAroundAdvisorChain`，按 `Ordered` 排序、Deque 弹出执行；**每个 advisor 的执行都被 `AdvisorObservationDocumentation.AI_ADVISOR` 观测包裹**（见 §5）。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/DefaultAroundAdvisorChain.java
- `ToolCallingAdvisor` 本身**为子类化设计**：protected 钩子 `doInitializeLoop/doBeforeCall/doAfterCall/doFinalizeLoop/doGetNextInstructionsForToolCall`（及对应 Stream 版本），Builder 用自引用泛型支持子类扩展。`conversationHistoryEnabled=false` 时仅携带最后一条历史+系统消息，需链内侧配 Memory advisor。
  - 源码：同上 ToolCallingAdvisor.java

**Harness 提示**：Advisor 链可以包住整个工具递归——官方观察模式就是把自定义 advisor 放到 `+400`（ToolCallingAdvisor 之后）以观测每次迭代。Harness 的记忆压缩、认知可观测可直接以"链内 advisor"落位；递归循环本身也可通过子类化 `ToolCallingAdvisor` 接管。

---

## 3. ChatMemory：接口、实现与读写时机

**接口（包 `org.springframework.ai.chat.memory`，模块 `spring-ai-model`）**

- `ChatMemory`（since 1.0.0）：`add(conversationId, Message/List<Message>)`、`get(conversationId)`、`clear(conversationId)`；上下文 key 常量 `ChatMemory.CONVERSATION_ID = "chat_memory_conversation_id"`。**2.0.0 起 conversationId 必填**：`DEFAULT_CONVERSATION_ID` 常量被移除，内存 advisor 构造器 `.conversationId(...)` 移除，缺省即抛 `IllegalArgumentException`。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/ChatMemory.java
  - 升级笔记：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"Chat Memory Advisors: Conversation ID Is Now Required"）
- `ChatMemoryRepository`（since 1.0.0）：`findConversationIds()`、`findByConversationId(id)`、**`saveAll(id, messages)`（整体替换该会话全部消息）**、`deleteByConversationId(id)`。实现：`InMemoryChatMemoryRepository`、JDBC（2.0 新增 `sequence_id` 列保证跨库确定性排序）、Cassandra、MongoDB、Neo4j、Redis（模块根目录 `memory-repositories/`）。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/ChatMemoryRepository.java
- `MessageWindowChatMemory`（since 1.0.0，**final 类**）：窗口淘汰——`SystemMessage` 永远保留；超窗时切口前移到最近的 `USER` 消息，保证窗口始于完整轮次；`maxMessages` 默认 20；`ChatMemoryRepository` 可插拔（Builder）。无 token 维度，无压缩概念。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/memory/MessageWindowChatMemory.java

**读写时机（`MessageChatMemoryAdvisor`，since 1.0.0）**

- 默认 order = `DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER`（+200），**在 ToolCallingAdvisor（+300）之外**：每用户轮次只读写一次，不参与每次工具迭代。
- `before()`：`chatMemory.get(conversationId)` 读出全部记忆消息并**前置注入 prompt**（含去重检测 `isMemoryAlreadyInPrompt`，SystemMessage 提首）；随后立即 `chatMemory.add(conversationId, userMessage)` 写入用户消息（取 `getLastUserOrToolResponseMessage()`）。
- `after()`：将最终响应的全部 `AssistantMessage` `chatMemory.add(...)` 写入。流式经 `ChatClientMessageAggregator` 聚合后同样走 after。
- 因为 memory advisor 在循环外，**工具调用中间消息（AssistantMessage.toolCalls / ToolResponseMessage）默认不会进 ChatMemoryRepository**（官方明确：多数 repository 不支持这些消息类型；只有 `InMemoryChatMemoryRepository` 支持）。若把 memory advisor order 调到 +400（循环内）并 `disableInternalConversationHistory()`，则每次迭代读写。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/MessageChatMemoryAdvisor.java
  - 文档：https://docs.spring.io/spring-ai/reference/api/advisors-recursive.html（"Conversation History Management"）
- 另有 `VectorStoreChatMemoryAdvisor`；`PromptChatMemoryAdvisor` 已在 2.0.0 移除。官方升级笔记提到社区项目 **spring-ai-session**（支持工具消息、计划在 2.1 取代 ChatMemory）。
  - 升级笔记：https://docs.spring.io/spring-ai/reference/upgrade-notes.html

**Harness 提示**：渐进式压缩的天然挂点 = 自定义 `ChatMemory`（包装 repository，get 时返回"压缩视图"）+ 自定义 memory advisor；`saveAll` 整体替换语义对压缩后回写友好；`ChatMemory` 接口本身极薄（3 方法），替换成本低。注意工具中间消息默认不落库——"完结轮次为压缩原子单位"与官方默认正好对齐。

---

## 4. 工具调用执行链：执行者、可替换性与结果接管

**执行者**

- `ToolCallingManager`（since 1.0.0，接口，`org.springframework.ai.model.tool`，模块 `spring-ai-model`）：`resolveToolDefinitions(ToolCallingChatOptions)` + **`executeToolCalls(Prompt, ChatResponse) → ToolExecutionResult`**。`ToolCallingManager.builder()` 得默认实现。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/model/tool/ToolCallingManager.java
- `DefaultToolCallingManager`（final class）：对 `assistantMessage.getToolCalls()` **逐个 for 循环顺序执行**（**无内置并行**）；每个工具调用被 `ToolCallingObservationDocumentation.TOOL_CALL` 观测包裹（span 名 `execute_tool <tool-name>`，2.0 起 `gen_ai.operation.name=execute_tool`，新增 `spring.ai.tool.type`、`spring.ai.tool.call.id` 属性）；结果按 tool_call 顺序追加进 `ToolResponseMessage`；`ToolExecutionExceptionProcessor` 决定异常转字符串还是抛出（默认 `DefaultToolExecutionExceptionProcessor`，可配 alwaysThrow）；`ToolCallbackResolver` 兜底按名解析未显式注册的回调；`returnDirect` 取所有工具的 AND。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/model/tool/DefaultToolCallingManager.java
  - 升级笔记（观测变更）：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"Observability — Tool Calling"）
- `ToolExecutionResult`：`conversationHistory()`（原 messages + assistantMessage + toolResponseMessage）+ `returnDirect()`。
- 2.0 中 `ToolCallingManager` 的调用方**只有** `ToolCallingAdvisor`（及 `ToolCallAdvisor`、`ToolSearchToolCallingAdvisor`）；ChatModel 内部执行已删除。

**能否替换/包装执行器？——可以，且是公开扩展点**

- `ToolCallingManager` 是接口，`ToolCallingAdvisor.builder().toolCallingManager(custom)` 直接注入；再经 `ChatClient.builder(...)` 五参重载或 Boot 的 `ToolCallingAdvisor.Builder<?>` Bean 全局生效。→ **自定义实现可在单点注入：虚拟线程并行执行多个 tool_call、大结果落盘（Spill）替换占位符、结果顺序/回注重排**。`ToolExecutionResult.conversationHistory` 由实现者自行构造，顺序完全可控。
- 更细粒度的包装点：`ToolCallback`（接口，`call(String)` / `call(String, ToolContext)` → `String`，since 1.0.0）——逐工具包装（装饰 `ToolCallbackProvider.getToolCallbacks()` 返回数组）即可做 Spill 接管而不动执行器。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/tool/ToolCallback.java
- `ToolCallResultConverter`（`org.springframework.ai.tool.execution`，since 1.0.0）：`String convert(Object result, Type returnType)`——仅用于**方法型工具**（`MethodToolCallback`）把返回值对象转字符串（默认 `DefaultToolCallResultConverter`，JSON 序列化）；不是通用结果接管点。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/tool/execution/ToolCallResultConverter.java
- `ToolCallingChatOptions`：options 层携带 `toolCallbacks`（运行时工具注入点）与 `toolContext`（调用级上下文，透传进 `ToolContext`，工具可读）；2.0 起 Options 严格不可变，`copy()` 移除、用 `mutate()`。
- 厂商 options 上的 `parallelToolCalls`（OpenAI）/`disableParallelToolUse`（Anthropic）只是**让模型一次性发出多个 tool_call** 的开关，与客户端并行执行无关。

**Harness 提示**：并行工具调用 = 自定义 `ToolCallingManager`（虚拟线程 fan-out，按序聚合）；Spill = 同一实现内在拼 `ToolResponseMessage` 前替换大结果（或包装 ToolCallback）；两者可组合成 `HarnessToolCallingManager implements ToolCallingManager`，经 Builder Bean 全局注入，与官方自动注册机制无缝兼容。

---

## 5. Observation：覆盖范围、自定义点与 reasoning 可得性

**Micrometer Observation 覆盖（官方 Observability 文档）**

| Span/Observation | 触发点 | 关键属性 |
|---|---|---|
| `spring.ai.chat.client`（`ChatClientObservationDocumentation.AI_CHAT_CLIENT`） | `ChatClient.call()/stream()` 整体，包住整条 advisor 链 | advisors 列表、conversation.id、tool.names（高基数） |
| `spring.ai.advisor`（`AdvisorObservationDocumentation.AI_ADVISOR`） | **每个 advisor 各一个 span**（含内层 advisor 耗时） | advisor.name、advisor.order |
| `gen_ai.client.operation`（ChatModel） | `ChatModel.call/stream` | gen_ai.* 语义约定、usage token（含 cache tokens）、finish_reasons |
| `execute_tool <tool-name>`（`ToolCallingObservationDocumentation.TOOL_CALL`） | 每次工具执行 | tool.name/type/id、arguments/result（默认关闭，`spring.ai.tools.observations.include-content=true` 开启） |
| HTTP 层 | OpenAI/Anthropic 的底层 HTTP client（注意：流式时 HTTP span 不挂在模型 span 下） | method/uri/status |
| embedding/image/vector store | 对应模型与向量库操作 | 见文档 |

- 来源：https://docs.spring.io/spring-ai/reference/observability/index.html
- prompt/completion 内容默认不导出，由 `spring.ai.chat.observations.log-prompt/log-completion`、`spring.ai.chat.client.observations.log-prompt/log-completion` 控制（日志而非 span 属性）。

**自定义点**

- 每层都有 Convention 接口可整体替换：`ChatClientObservationConvention`（Boot 自动配置 `@ConditionalOnMissingBean` 注入）、`AdvisorObservationConvention`（经 `ChatClient.builder` 四参注入）、`ChatModelObservationConvention`（各模型 `setObservationConvention`）、`ToolCallingObservationConvention`（`DefaultToolCallingManager.setObservationConvention`）。`ObservationRegistry` 全链路可注入。
  - Javadoc 例：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/observation/DefaultChatClientObservationConvention.html
- 另有 `ChatClientCompletionObservationHandler`、`ChatClientPromptContentObservationHandler`、`ToolCallingContentObservationFilter` 等 Handler/Filter 挂点；Micrometer `ObservationHandler`/`ObservationFilter` 原生机制当然可用（Harness 可注册自定义 Handler 把 Span/Event 落自己的存储，不动官方约定）。

**reasoning/thinking 在抽象层的可得性：无统一 API，走 `AssistantMessage.getMetadata()` 厂商各自 key**

- OpenAI 兼容服务器（DeepSeek、vLLM、Ollama 的 OpenAI 端点）：metadata key **`"reasoningContent"`**；DeepSeek 另有 `DeepSeekAssistantMessage extends AssistantMessage` 提供 `getReasoningContent()`。
- Ollama 原生端点 thinking 模式：metadata key **`"thinking"`**（options 侧 `spring.ai.ollama.chat.think`）。
- Mistral AI：**`"thinking_content"`、`"reference_content"`、`"reference_thinking_content"`**（options 侧 `reasoningEffort`）。
- Anthropic：thinking 块进入 `Generation`/`AssistantMessage` 结构，thinking 块 metadata 带 **`"signature"`**，流式 thinking delta 带 **`"thinking"`** key（options 侧 `spring.ai.anthropic.chat.thinking`，display 可配 SUMMARIZED/OMITTED）。
- 官方 OpenAI 模型（GPT-5/o1/o3）：**不返回推理文本**，仅 usage 里有 reasoning_tokens 计数——抽象层拿不到思维链内容。
- 来源：DeepWiki（spring-projects/spring-ai，基于 2.0.0 源码）：https://deepwiki.com/search/how-does-spring-ai-expose-mode_b8ca2fb7-8742-449b-aef8-98ae62edc1c6 ；各 options Javadoc（如 https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/openai/OpenAiChatOptions.html 的 `getReasoningEffort()`）

**Harness 提示**：认知可观测的 Span 骨架可直接复用官方三层 span（chat_client ⊃ advisor ⊃ chat model/tool），自研 Event（思维链、证据、结论）通过自定义 advisor（循环内 +400）+ 自定义 ObservationHandler 落地；思维链采集必须按厂商 metadata key 适配（Anthropic signature/thinking、DeepSeek reasoningContent、Mistral thinking_content、Ollama thinking），抽象层无统一 getter，是 Harness 需要自己抹平的一层。

---

## 6. MCP：client 现状、工具注册与运行时增删

**现状**

- starter：`spring-ai-starter-mcp-client`（HttpClient/JDK 传输）与 `spring-ai-starter-mcp-client-webflux`；传输方式：STDIO、Streamable HTTP、SSE（**SSE 传输 2.0.0 起 `@Deprecated(forRemoval=true)`**）。
  - 文档：https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
  - Javadoc（SSE deprecated）：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/mcp/client/webflux/autoconfigure/SseWebFluxTransportAutoConfiguration.html
- 配置驱动：`spring.ai.mcp.client.stdio.connections.<name>.*`、`spring.ai.mcp.client.streamable-http.connections.<name>.url`（map 结构，支持多命名连接）；公共属性 `spring.ai.mcp.client.{enabled,type(SYNC/ASYNC),name,version,request-timeout,initialized}`。
  - Javadoc：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/mcp/client/common/autoconfigure/properties/McpStreamableHttpClientProperties.html
- 注册链路：`NamedClientMcpTransport` Bean（每连接一个）→ `McpClientAutoConfiguration` 为每个 transport 建 `McpSyncClient`/`McpAsyncClient`（`List<McpSyncClient>` Bean；可用 `McpClientCustomizer<McpClient.SyncSpec>` 定制 spec；支持 sampling/elicitation/logging/progress/tools-changed 等回调注册）→ `McpToolCallbackAutoConfiguration` 建 **`SyncMcpToolCallbackProvider`**（ToolCallbackProvider Bean，ChatClient 直接可用）。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/auto-configurations/mcp/spring-ai-autoconfigure-mcp-client-common/src/main/java/org/springframework/ai/mcp/client/common/autoconfigure/McpClientAutoConfiguration.java
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/auto-configurations/mcp/spring-ai-autoconfigure-mcp-client-common/src/main/java/org/springframework/ai/mcp/client/common/autoconfigure/McpToolCallbackAutoConfiguration.java
- 工具发现细节：`SyncMcpToolCallbackProvider.getToolCallbacks()` 向每个 client `listTools()` 聚合并缓存；实现 `ApplicationListener<McpToolsChangedEvent>`——server 推送 tools/list_changed 通知 → `McpSyncToolsChangeEventEmmiter` 发 Spring 事件 → **缓存失效、下次调用重新发现**。工具过滤 `McpToolFilter`、命名前缀 `McpToolNamePrefixGenerator`（默认 `DefaultMcpToolNamePrefixGenerator`，可 noPrefix）、`ToolContextToMcpMetaConverter` 均可插拔；重名工具直接 `IllegalStateException`。
  - 源码：https://github.com/spring-projects/spring-ai/blob/v2.0.0/mcp/common/src/main/java/org/springframework/ai/mcp/SyncMcpToolCallbackProvider.java
- 底层 MCP Java SDK：`io.modelcontextprotocol.sdk` **2.0.0**。

**运行时动态增删 MCP server/工具的可行性**

- **server 侧工具集变更**：已支持（上述事件驱动缓存失效），这是公开机制。
- **运行时增删 server 连接**：自动配置**没有公开 API**——`List<McpSyncClient>` 在启动期由 properties 一次性建好，随 context 关闭统一 `close()`。可行路径（均为公开类手工组合，非官方 API）：① 自建 `McpClient.sync(transport).build()`（transport 用 `HttpClientStreamableHttpTransport`/`StdioClientTransport` 等公开类）注册进自己维护的 `ToolCallbackProvider`/registry，实现差量刷新与引用计数延迟关闭；② 重建 `SyncMcpToolCallbackProvider`（Builder 公开，`addMcpClient` 仅构造期有效）并触发 `invalidateCache()`。结论：**热插拔必须由 Harness 自建 client 生命周期注册表**，Spring AI 只提供全部原材料。
- 另注：`ToolSearchToolCallingAdvisor`（2.0，`spring-ai-starter-tool-search-advisor`，regex/lucene/vector 三种工具索引）可替换默认 ToolCallingAdvisor，**每次调用只把最相关工具定义发给 LLM**——与 Skill"清单先行、按需取正文"的机制高度同构，可参考/复用。
  - 升级笔记：https://docs.spring.io/spring-ai/reference/upgrade-notes.html（"New: ToolSearchToolCallingAdvisor Auto-Configuration and Starter"）；指南：https://docs.spring.io/spring-ai/reference/guides/dynamic-tool-search.html

---

## 7. 多模型思维链：统一程度盘点（2.0.0）

| 厂商 | options 层开关 | 推理内容暴露 | 抽象层统一性 |
|---|---|---|---|
| Anthropic | `spring.ai.anthropic.chat.thinking`（ThinkingConfigParam，display SUMMARIZED/OMITTED） | thinking 块（metadata `signature`），流式 delta metadata `thinking` | 厂商私有 |
| OpenAI（官方） | `reasoningEffort`、`verbosity` | **无推理文本**，仅 usage reasoning_tokens | 拿不到 |
| OpenAI 兼容（DeepSeek/vLLM/Ollama-OpenAI 端点） | 同上 | metadata `reasoningContent`；DeepSeek 有 `DeepSeekAssistantMessage.getReasoningContent()` | 事实标准 key，但非接口 |
| Google GenAI | `ThinkingConfig`（`thinkingBudget`、`thinkingLevel`(MINIMAL/LOW/MEDIUM/HIGH)、`includeThoughts`） | thoughts 内容（options 控制是否返回） | 厂商私有 |
| Mistral AI | `reasoningEffort` | metadata `thinking_content` / `reference_content` / `reference_thinking_content` | 厂商私有 |
| Ollama（原生） | `spring.ai.ollama.chat.think` | metadata `thinking` | 厂商私有 |
| 阿里 DashScope（通义） | — | — | **不在 Spring AI 核心**（在 Alibaba 社区项目 spring-ai-alibaba） |
| MiniMax | — | 2.0 移除专用模块，改走 Anthropic 兼容端点 | — |

- 结论：**抽象层没有统一 reasoning API**；`AssistantMessage.getMetadata()` 是唯一通用通道，各厂商 key 不同（`reasoningContent`/`thinking`/`thinking_content`/thinking 块）。Bedrock Converse 的 thinking 支持本次未验证（留待模型专项调研）。
- 来源：DeepWiki 问答（基于 2.0.0 源码）：https://deepwiki.com/search/how-does-spring-ai-expose-mode_b8ca2fb7-8742-449b-aef8-98ae62edc1c6 ；Javadoc：https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/google/genai/common/GoogleGenAiThinkingLevel.html ；升级笔记（MiniMax）：https://docs.spring.io/spring-ai/reference/upgrade-notes.html

---

## 附：对 Harness 各机制挂接点的直接推论

| Harness 机制 | 推荐挂点（基于上述事实） |
|---|---|
| 渐进式记忆压缩/微压缩 | 自定义 `ChatMemory` 实现（get 返回压缩视图）+ 自定义 memory advisor（默认 +200 循环外，与"完结轮次"原子单位对齐）；`ChatMemoryRepository.saveAll` 整体替换语义适合压缩回写；悬空调用修复可在自定义 repository/advisor 加载路径做 |
| Spill 溢出保护 | 自定义 `ToolCallingManager.executeToolCalls`：拼 `ToolResponseMessage` 前大结果落盘换占位符；或逐工具包装 `ToolCallback.call` 返回值；回读工具注册为普通 `ToolCallback`（支持范围读取） |
| 并行工具调用 | 自定义 `ToolCallingManager`（虚拟线程 fan-out + 按 tool_call 顺序聚合），经 `ToolCallingAdvisor.Builder`/Boot Bean 注入；官方默认实现为顺序 for 循环，无竞争点 |
| Span+Event 认知可观测 | 复用官方三层 span + 循环内观测 advisor（order +400，每迭代可见含 ToolResponseMessage 的完整请求）+ 自定义 `ObservationHandler`/convention 落自研存储；思维链按厂商 metadata key 适配采集 |
| Skill 按需加载 | 工具清单/正文两级加载可参考 `ToolSearchToolCallingAdvisor`；动态工具集经 `ToolCallingChatOptions.toolCallbacks` 运行时注入或自定义 `ToolCallbackProvider` |
| MCP 热插拔 | 自建 `McpSyncClient` 生命周期 registry（官方无运行时增删 API）；工具集变更可复用 `McpToolsChangedEvent`→缓存失效机制；差量刷新+引用计数延迟关闭须自研 |
