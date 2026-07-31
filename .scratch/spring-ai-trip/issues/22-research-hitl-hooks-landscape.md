# HITL 与 Hook 机制行业调研

Type: research
Status: resolved
Blocked by: —

## Question

为「Hook 护栏体系」（参照腾讯 DECO 文章，全文在 `.scratch/spring-ai-trip/research/hooks-article.md`）的设计收集行业事实：

1. **Spring AI 现状**：Spring AI 2.x 有无原生 HITL（human-in-the-loop）/工具确认机制？有无官方 Hook/Callback 抽象（还是只有 Advisor 链）？工具执行前的拦截与阻断在 2.x 工具调用循环（ToolCallingAdvisor/ToolCallingManager）里如何表达——能否在调工具前暂停一轮、等外部输入后续跑（`ChatClient` 的暂停/恢复语义）？
2. **ADK（Java/Python）**：8 大 Hook 模式的官方定义；`ToolConfirmation`（1.0.0+）的暂停/恢复与防循环机制；`ArtifactService` 的 save/load 与 context offloading 示例。
3. **LangGraph**：HITL Middleware（`interruptOn`、approve/edit/reject/respond）、checkpointer 暂停/恢复；DeepAgents 的 Large Tool Result Offloading（>20k token 落盘留指针）与 SummarizationMiddleware（85% 容量自动摘要）细节。
4. **Claude Code**：`PreToolUse` hook 的 permissionDecision（allow/deny/ask）与 settings.json 配置；SessionStart/UserPromptSubmit 注入机制。
5. **服务端 HITL 的暂停/恢复工程问题**：DECO 的「发事件 + state 授权标记 + LLM 重试放行」vs LangGraph 的「checkpointer 挂起整个图」——在分布式多实例 Spring Boot 服务里，哪种语义可落地？挂起等待期间连接/线程怎么办（SSE 长连接 vs 轮询续跑）？

产出：事实清单（带来源链接与版本号）写入 `.scratch/spring-ai-trip/research/hitl-hooks-landscape.md`；200 字内摘要追加到本 ticket `## Answer` 并将 Status 改为 resolved。不要修改其他任何文件（尤其 map.md）。

## Answer

Spring AI 2.0 无原生 HITL 与 Callback 体系，只有 Advisor 链；`ToolCallingAdvisor` 提供 doBefore/AfterCall 等子类钩子，或用 `toolCallingAdvisorAutoRegister(false)` 手动循环拦截工具执行，ChatClient 无暂停/恢复，须自行持久化 history 续跑。ADK 有 8 种 Callback、ToolConfirmation（1.0.0+，框架管暂停/恢复/防循环）与 ArtifactService offload 示例。LangGraph `interrupt()`+checkpointer 挂起不占线程、节点级重放；HITL Middleware 配 interruptOn（approve/edit/reject）；DeepAgents 工具结果 >20k token 落盘留指针、85% 容量自动摘要。Claude Code PreToolUse 输出 permissionDecision（allow/deny/ask/defer），SessionStart/UserPromptSubmit 经 stdout 与 additionalContext 注入。工程结论：挂起=状态外置+释放连接，DECO 式重放在 Spring AI 上成本最低；Java 先例为 Spring AI Alibaba graph（InterruptableAction+CheckpointSaver）。详见 research/hitl-hooks-landscape.md。
