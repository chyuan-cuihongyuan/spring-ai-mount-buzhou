# HITL 与 Hook 机制行业事实清单

> 调研时间：2026-07-31。为「Hook 护栏体系」设计 Spec 收集的行业事实，参照 `.scratch/spring-ai-trip/research/hooks-article.md`（腾讯 DECO 全文存档）。
> 所有结论标注来源与版本号；「推论」与「事实」分开标注。

---

## 1. Spring AI 2.x 现状

**版本坐标**：1.0.x 为 GA 线；2.0 尚处里程碑（截至调研时 context7 收录 2.0.0-M3/M6，官方参考文档 2.0-SNAPSHOT）。2.0 相对 1.x 的重大变化：工具调用循环从各 `ChatModel` 内部（1.x 的 `internalToolExecutionEnabled`，已删除）上移到 Advisor 链，由 `ToolCallingAdvisor` 统一驱动（取代已废弃的 `ToolCallAdvisor`）。

### 1.1 有无原生 HITL / 工具确认机制？

**事实：没有。** Spring AI 2.0 无开箱即用的 HITL / 工具确认抽象（无 approve/deny 配置项、无确认事件类型、无暂停原语）。官方文档提供的最近路径是「User-Controlled Tool Execution」：关闭自动工具循环，自己驱动 while 循环，在循环里插入任意检查/确认逻辑。
来源：<https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools.html>（"User-Controlled Tool Execution" 节）；DeepWiki spring-projects/spring-ai 检索（2026-07）。

### 1.2 有无官方 Hook/Callback 抽象？

**事实：只有 Advisor 链，没有 ADK 式 Callback 体系。** 扩展点是 `CallAdvisor` / `StreamAdvisor` 接口 + `ChatClientRequest/Response`。但 2.0 的 `ToolCallingAdvisor`（`org.springframework.ai.chat.client.advisor.ToolCallingAdvisor`）暴露了 4 对 protected 钩子方法，可子类化挂载护栏：

- `doInitializeLoop` / `doInitializeLoopStream`
- `doBeforeCall` / `doBeforeStream`（每次模型调用前，含工具循环内每一次）
- `doAfterCall` / `doAfterStream`
- `doFinalizeLoop` / `doFinalizeLoopStream`

`ToolCallingAdvisor` 是「recursive advisor」：通过 `CallAdvisorChain.copy(this)` 创建子链，让下游 Advisor 能观察/拦截每一次工具迭代。
来源：Javadoc <https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/ToolCallingAdvisor.html>；参考文档 <https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools/tool-calling-advisor.html>。

### 1.3 工具执行前的拦截与阻断如何表达？

**事实：三种官方路径，均无「deny 语义」一等公民，需自行表达：**

1. **自定义 Advisor / 子类化 `ToolCallingAdvisor`**：在 `doBeforeCall` 里检查/改写请求；阻断只能靠短路返回伪造响应或抛异常。
2. **手动驱动循环**（官方推荐用于 HITL 场景的形态）：
   ```java
   ChatClientResponse response = chatClient.prompt()
       .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
       .call().chatClientResponse();
   while (response.chatResponse() != null && response.chatResponse().hasToolCalls()) {
       // ← 拦截点：检查工具调用，可暂停、转发 SSE、等确认
       ToolExecutionResult result = toolCallingManager.executeToolCalls(prompt, response.chatResponse());
       prompt = new Prompt(result.conversationHistory(), chatOptions);
       response = chatClient.prompt().messages(result.conversationHistory())
           .advisors(AdvisorParams.toolCallingAdvisorAutoRegister(false))
           .call().chatClientResponse();
   }
   ```
   `ToolExecutionResult.conversationHistory()` 返回可序列化的完整历史，是天然的「续跑载体」。
3. **`ToolExecutionEligibilityChecker`**（函数式接口）：判定模型响应是否应触发下一轮工具迭代，默认实现检查 `hasToolCalls()`，可定制（如识别 provider 特定 stop-reason）。

来源：<https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools.html>、<https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/upgrade-notes.html>（"Manual Tool Execution Loop"）。

### 1.4 ChatClient 有无暂停/恢复语义？

**事实：没有内建暂停/恢复。** 无 checkpointer、无 interrupt 原语、无 thread/run 概念。暂停/恢复只能由上层在手动循环中实现：把 `conversationHistory` 持久化（可配合 `ChatMemoryRepository`），释放请求线程，外部输入到达后用持久化历史重新 `prompt()` 续跑。**推论**：这正是 DECO 式「state 授权标记 + 续跑入口」在 Spring AI 上的自然落地形态。

---

## 2. Google ADK（Python / Java）

### 2.1 官方 Hook（Callback）体系与 8 种模式

**事实：ADK 有 8 种 Callback 类型**（Python ADK；Java ADK 同构）：

| Callback | 时机 | 短路语义 |
|---|---|---|
| `before_agent_callback` / `after_agent_callback` | Agent 运行前/后 | before 返回非空 → 跳过 agent 运行 |
| `before_model_callback` / `after_model_callback` | LLM 调用前/后 | before 返回 `LlmResponse` → 跳过实际 LLM 调用；after 返回值覆盖响应 |
| `before_tool_callback` / `after_tool_callback` | 工具执行前/后 | before 返回 dict → **跳过工具执行**，该 dict 作为工具输出；after 返回 dict 覆盖工具结果 |
| `on_model_error_callback` / `on_tool_error_callback` | LLM/工具出错时 | — |

回调接收 `CallbackContext`（`Context` 别名），可读写 state、调 `save_artifact()` / `load_artifact()` / `request_confirmation()`。

**8 种官方设计模式**（ADK 文档 "Callbacks" 章）：① 防护栏与策略执行（Guardrails & Policy Enforcement）② 动态状态管理（Dynamic State Management）③ 日志与监控（Logging & Monitoring）④ 缓存（Caching）⑤ 请求/响应修改（Request/Response Modification）⑥ 条件跳过步骤（Conditional Skipping of Steps）⑦ 认证与摘要控制（Tool-level Authentication / Summarization Control）⑧ 工件处理（Artifact Handling）。
来源：DeepWiki google/adk-python（callbacks-and-plugins.md、architecture/context.md）；DECO 文章 §6.1 转述 ADK 官方分类（<https://google.github.io/adk-docs/>）。

**Java ADK**：回调为函数式接口（`BeforeModelCallback`/`AfterModelCallback`/`BeforeToolCallback`/`AfterToolCallback`/`OnModelErrorCallback`/`OnToolErrorCallback`），各有 Sync（返回 `Optional`）与异步（返回 `Maybe`）变体；`PluginManager` 聚合执行所有插件回调；可通过 YAML 配置 + `ComponentRegistry` 装配。
来源：DeepWiki google/adk-java（2026-07）。

### 2.2 ToolConfirmation（1.0.0+）暂停/恢复与防循环

**事实（Python ADK 1.0.0 起内置；DECO 文章称当前最新 1.4.0）：**

1. **发起确认**：工具内调 `tool_context.request_confirmation(hint, payload)`；或 `FunctionTool(require_confirmation=True)` 自动对每次调用请求确认。
2. **暂停**：flow yield 一个含特殊 `FunctionCall`（名 `adk_request_confirmation`，常量 `REQUEST_CONFIRMATION_FUNCTION_CALL_NAME`）的 Event，Agent 执行暂停，等待用户输入。
3. **恢复**：用户以 `FunctionResponse` 回填 `ToolConfirmation{confirmed: bool, payload}`。
4. **防循环（关键）**：`_RequestConfirmationLlmRequestProcessor`（`src/google/adk/flows/llm_flows/request_confirmation.py`）——`_resolve_confirmation_targets` 定位原始 function call；**移除已确认的工具调用避免二次执行**；把确认 payload 注入 `tool_context` 后重新执行该工具。即：框架自动清理中间事件 + 注入已确认 call，开发者无需处理「重放导致重复确认」。

来源：DeepWiki google/adk-python "Tool Confirmation (HITL)" wiki、`tests/unittests/runners/test_run_tool_confirmation.py`；DECO 文章 §4.4。

**Java ADK**：`toolContext.requestConfirmation()` 同义（更新 `EventActions`），暂停后产生 `adk_request_confirmation` function call 事件；`Functions.hasPendingLongRunningCall()` 对应 Python 的 `should_pause_invocation`；另有 `ResumabilityConfig` / `WorkflowAgentResumption` 支持长任务以 taskId 恢复。
来源：DeepWiki google/adk-java。

### 2.3 ArtifactService 与 context offloading 示例

**事实：**

- `BaseArtifactService`：`save_artifact(filename, artifact, session_id, custom_metadata)` → 返回版本号；`load_artifact(filename, session_id, version)` → `types.Part`。按 app / user / session 三级作用域隔离。
- 官方示例 `context_offloading_with_artifact`（`contributing/samples/patterns/context_offloading_with_artifact/`）：
  1. `query_large_data` 工具生成大报告 → `tool_context.save_artifact()` 落 artifact，摘要写入 `custom_metadata`，**工具返回只含小摘要**（当前轮事件保持小）；
  2. 工具的 `process_llm_request` 钩子检测到刚保存的报告 → `load_artifact()` 把全文注入**下一次 LLM 请求，仅当前轮可见，不进 session 历史**；
  3. `CustomLoadArtifactsTool`（extends `LoadArtifactsTool`）把可用 artifact 清单+摘要写进 instructions，LLM 需要时主动调 `load_artifacts` 按需取回。

来源：DeepWiki google/adk-python "Artifact Storage" wiki、示例 README/agent.py。

---

## 3. LangChain / LangGraph / DeepAgents

### 3.1 HITL Middleware（LangChain 1.x）

**事实：** `HumanInTheLoopMiddleware`（`langchain.agents.middleware`），声明式配置：

```python
HumanInTheLoopMiddleware(
    interrupt_on={
        "send_email_tool": {"allowed_decisions": ["approve", "edit", "reject"]},
        "read_email_tool": False,   # 自动放行
    }
)
```

- `interrupt_on`：工具名 → `True`（默认配置中断）/ `False`（放行）/ `InterruptOnConfig{allowed_decisions, description}`。
- 决策类型：官方文档列 `approve` / `edit` / `reject` 三种（早期 HITL 文档与 DeepAgents `review_configs` 中另有 `respond`——允许人工直接给文本反馈改写后续行为；DECO 文章按四类列举）。
- **必须配 checkpointer**（如 `InMemorySaver`），中断即落 checkpoint。
- 子 agent 可继承/覆写 `interrupt_on`（DeepAgents 封装 `HumanInTheLoopMiddleware` 时处理继承）。

来源：<https://docs.langchain.com/oss/python/langchain/middleware#human-in-the-loop>（2026-07 抓取）；DeepWiki langchain-ai/deepagents。

### 3.2 checkpointer 暂停/恢复实现语义（LangGraph）

**事实（docs.langchain.com/oss/python/langgraph/interrupts）：**

- `interrupt()` 是**动态中断**：节点内任意位置调用，payload 须 JSON 可序列化。机制：抛出特殊异常冒泡到 runtime → checkpointer 保存完整图状态 → **无限期等待**，不占线程。
- 恢复：以**相同 `thread_id`** 重新调用图并传 `Command(resume=<值>)`；该值成为节点内 `interrupt()` 调用的返回值。
- **节点级重放语义（关键工程约束）**：恢复时**整个节点从头重跑**，`interrupt()` 之前的代码会再次执行 → 副作用必须幂等或放在 `interrupt()` 之后；同一节点多个 interrupt 严格按**索引顺序**匹配 resume 值；不得用 try/except 包裹 `interrupt()`。
- 静态断点：`compile(interrupt_before=[...], interrupt_after=[...])` 或运行时同参，恢复时传 `None`。
- 流式：`graph.stream_events(..., version="v3")` 暴露 `stream.interrupted` / `stream.interrupts`。

**推论**：LangGraph 的「挂起整个图」≠ 占住线程/连接，而是「持久化 + 断开 + 新请求恢复」，与 DECO 的续跑模型在分布式语义上同构；差别在恢复点精度（checkpoint 精确恢复 vs DECO 重放一轮 LLM）。

### 3.3 DeepAgents：Large Tool Result Offloading 与 SummarizationMiddleware

**事实（langchain-ai/deepagents 源码，`libs/deepagents/deepagents/middleware/filesystem.py`）：**

- `FilesystemMiddleware(tool_token_limit_before_evict: int | None = 20000, human_message_token_limit_before_evict: int | None = 50000, ...)`：**工具结果超过 20000 token 即落盘**（写入配置的 backend），消息中替换为**截断预览 + 文件引用**；超长 HumanMessage 阈值 50000。2026 年社区 issue #4749 要求把该参数提到 `FilesystemConfig` 顶层可配。
- `SummarizationMiddleware`（基类在 `langchain.agents.middleware.summarization`）：DeepAgents 扩展版按模型 profile 计算默认值——有 `max_input_tokens` 时 **`trigger=("fraction", 0.85)`（85% 容量触发自动摘要）**，`keep=("fraction", 0.10)`（保留最近 10%）；被逐出历史批量落盘到 backend（`/conversation_history/{thread_id}.md`，媒体单独引用）；另有 `TruncateArgsSettings` 在摘要前以更低阈值截断大工具入参（write_file/edit_file）。基类版参数：`max_tokens_before_summary`（如 4000）、`messages_to_keep`（如 20）。

来源：deepagents 源码直读（2026-07）；<https://docs.langchain.com/oss/python/langchain/middleware>（Summarization 节）；DeepWiki langchain-ai/deepagents；DECO 文章 §3.5（">20k token 落盘留指针 / 85% 容量自动摘要"与源码一致）。

---

## 4. Claude Code Hooks

来源：<https://code.claude.com/docs/en/hooks>（2026-07-31 抓取；文中引用 v2.1.141–v2.1.214 行为注记）。

### 4.1 PreToolUse 与 permissionDecision

**事实：**

- **触发点**：Claude 生成工具参数后、执行前。`matcher` 按工具名过滤（`Bash`/`Edit`/`Write`/`Read`/`Glob`/`Grep`/`Agent`/`WebFetch`/`WebSearch`/`AskUserQuestion`/`ExitPlanMode`/任意 MCP 工具名）；handler 级 `if` 字段可按参数细筛（权限规则语法，如 `"Bash(git *)"`）。
- **输入**：stdin 收 JSON（`tool_name`、`tool_input`、`tool_use_id`、`session_id`、`cwd` 等）。
- **决策输出**（stdout JSON）：PreToolUse 不用顶层 `decision`，而用 `hookSpecificOutput`：
  ```json
  {
    "hookSpecificOutput": {
      "hookEventName": "PreToolUse",
      "permissionDecision": "allow" | "deny" | "ask" | "defer",
      "permissionDecisionReason": "...",
      "updatedInput": { ... }
    }
  }
  ```
  `allow`（可连带 `updatedInput` 改写入参）/ `deny` / `ask`（交回用户确认）/ `defer`（v2.1.199+，进程退出 `stop_reason:"tool_deferred"`，外部 UI 收答案后 `--resume` 重放，hook 再返 allow+updatedInput）。`PostToolUse` 对称支持 `updatedToolOutput` 改写工具结果。
- **退出码语义**：exit 0 + stdout JSON 按上述处理；**exit 2 = 阻断错误**，stderr 文本喂给 Claude（PreToolUse 下即阻断工具调用）；其他退出码为非阻断错误。stdout 超 10000 字符自动落盘、留预览+路径（与大工具结果同款处理）。

### 4.2 settings.json 配置格式

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [{ "type": "command", "command": ".claude/hooks/block-rm.sh" }]
      }
    ]
  }
}
```

三层嵌套：事件 → matcher 组 → handler 列表。handler 类型：`command` / `http` / `prompt` / `agent` / `mcp_tool`。配置位置：user / project / local / managed / plugin，**跨层级合并**而非覆盖；`disableAllHooks`、企业 `allowManagedHooksOnly` 可管控。HTTP hook 可 POST 到本地校验服务，2xx + `hookSpecificOutput.permissionDecision:"deny"` 即拒绝。

### 4.3 SessionStart / UserPromptSubmit 注入机制

- **SessionStart**：每会话一次（matcher 区分 startup/resume/clear/compact 来源）；**stdout 直接注入为 Claude 可见上下文**；用于加载开发上下文（issue、近期变更）。
- **UserPromptSubmit**：每轮一次；stdout 注入上下文；exit 2 可拒绝该 prompt。
- **`additionalContext`**（`hookSpecificOutput` 内）：字符串被包成 system-reminder，在 hook 触发点插入对话（SessionStart→会话开头；UserPromptSubmit→随 prompt；Pre/PostToolUse→贴工具结果旁；Stop→轮末）。多 hook 同事件全部注入；单值超 10000 字符落盘留路径+预览。
- 事件节奏：每会话（SessionStart/SessionEnd）、每轮（UserPromptSubmit/Stop/StopFailure）、每次工具调用（PreToolUse/PostToolUse）。

---

## 5. 服务端 HITL 暂停/恢复的工程问题

### 5.1 两种范式对比（事实 + 推论）

| 维度 | DECO 式（事件 + state 授权标记 + LLM 重试放行） | LangGraph 式（checkpointer 挂起整图） |
|---|---|---|
| 暂停语义 | beforeTool Hook 阻断本轮，发 SSE 事件，请求结束 | `interrupt()` 抛异常，checkpoint 落库，调用返回 |
| 恢复语义 | 前端写 `session.state[requiredState]` → **重新驱动同一会话新一轮**，LLM 重调工具时守卫放行（**重放式**） | 同 `thread_id` + `Command(resume=...)`，从 checkpoint 精确恢复，节点从头重跑（**恢复式 + 节点重放**） |
| 线程/连接 | 等待期间不占线程 | 同样不占线程（"waits indefinitely"指逻辑等待，非阻塞线程） |
| 防循环/防重复 | `requiredState` key 标记已授权 | 框架清理中间事件 + 索引序匹配 resume 值 |
| 状态载体 | 会话存储（session.state + 持久化历史） | checkpointer（MemorySaver/DB/Redis） |

**推论**：在分布式多实例 Spring Boot 里两者**都可落地**，前提相同——挂起 = 状态外置（DB/Redis）+ 释放线程；恢复 = 任意实例凭 sessionId/threadId 重放或恢复。LangGraph 式要求框架有 checkpoint/replay 能力（Spring AI 2.0 没有）；DECO 式只需「可序列化历史 + 续跑入口 + state 判定」，**Spring AI 2.0 手动循环（§1.3-2）恰好提供全部原料**：`ToolExecutionResult.conversationHistory()` 持久化 + 拦截点暂停 + state 标记放行。对叠加式 Harness，DECO 式是改造成本最低的路径。

### 5.2 挂起等待期间连接/线程处理

**事实与推论：**

- **DECO 实践（事实）**：确认事件走 SSE 管道（`INTERACTION_BOX` 为 `CUSTOM` 事件子类型）发给前端后，**当前续跑请求结束**；用户点选项 → REST API 写会话 state → **再发起一次续跑请求**重新驱动会话。即「SSE 推送事件 + 断开 + 新请求续跑」，不持有长连接。
- **LangGraph（事实）**：文档推荐事件流驱动，interrupt 后流结束，恢复是新的 `stream_events` 调用；`thread_id` 是持久游标，天然支持跨实例、跨连接恢复。
- **可选方案对比（推论）**：(a) 事件推送 + 断开 + 客户端再请求（DECO/官方推荐，代理/超时友好）；(b) SSE 长连接挂起等待（需心跳、受 LB/proxy 超时制约、实例故障即断，分布式下还要 sticky session 或广播找回，不推荐）；(c) 轮询续跑状态（简单但延迟高）。结论：**挂起期必须「无连接、无线程」，确认动作本身是独立写请求**。
- **确认写入的并发安全（推论）**：授权标记写入与续跑触发应原子（同事务/同请求），否则多实例下可能出现「state 未落库续跑已到另一实例」。

### 5.3 Java 生态先例

**事实：Spring AI Alibaba（alibaba/spring-ai-alibaba）已实现 LangGraph 式 HITL：**

- 模块 `spring-ai-alibaba-graph-core` / `spring-ai-alibaba-agent-framework`；发布线 1.0.x / 1.1.x（tag 至 v1.1.2.2），另有 v2.0.0-M1.1 里程碑。
- `InterruptableAction` 接口：`interrupt()`（节点执行前）/ `interruptAfter()`（执行后、结果合并前）；中断产出 `InterruptionMetadata`（nodeId、`OverAllState`、工具反馈）。
- `CompileConfig.interruptBefore()/interruptAfter()` 全局断点；`CompiledGraph` 中断时由 `CheckpointSaver` 落 `OverAllState`；实现有 `MemorySaver`、JDBC `DatabaseStore`（MySQL/PostgreSQL/H2）等。
- 恢复：`RunnableConfig.Builder.resume()` + `addHumanFeedback(InterruptionMetadata)`；`HumanInTheLoopHook`（AFTER_MODEL 位）按 `FeedbackResult`（APPROVED/EDITED/REJECTED）改写 `AssistantMessage` 工具调用或补 `ToolResponseMessage`。
- 测试样例：`StateGraphMemorySaverTest`、`HumanInTheLoopTest`、`TimeTravelTest`（历史 checkpoint 回放）。

来源：DeepWiki alibaba/spring-ai-alibaba（2026-07）；GitHub tags（git ls-remote，2026-07-31）。

**事实补充**：Spring AI 本体仅提供 `ChatMemoryRepository`（消息持久化），**无图级 checkpoint/恢复语义**；Spring AI Alibaba 的 graph 线是 Java 生态目前唯一成形的 checkpointer+HITL 先例，但其是「另起图引擎」而非「叠加在 ChatClient 上的 Harness」——本项目定位（叠加层）与之不同，更贴近 DECO 式。

---

## 6. 对本项目设计的直接输入（推论汇总）

1. **Hook 抽象要自己建**：Spring AI 2.0 只有 Advisor 链；`ToolCallingAdvisor` 子类钩子（doBeforeCall/doAfterCall 等）+ 手动循环是唯二官方切面。Harness 应在其上定义 ADK 式 `beforeTool/afterTool/beforeModel/afterModel` Callback 契约。
2. **HITL 走 DECO 式重放**：beforeTool 阻断 → 持久化 conversationHistory + 发事件 → 释放连接 → 确认写 state → 续跑重放放行。原料在 Spring AI 2.0 全部具备。
3. **防循环可借鉴 ADK ToolConfirmation**：恢复时清理中间事件/注入已确认 call，或 DECO 的 requiredState 授权标记，二选一或叠加。
4. **offload 阈值有行业锚点**：DeepAgents 20000 token（工具结果）/ 50000（用户消息）/ 85% 容量触发摘要 / 保留 10%——可直接作为默认值起点。
5. **注入机制参照 Claude Code additionalContext**：Hook 产出包成 reminder 在下一轮 prompt 注入，超阈值落盘留路径——与 DECO Hook→state→Attachment 闭环同构。
